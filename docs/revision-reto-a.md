# Reporte de Revisión de Código — Módulo `reto-a`

**Proyecto:** Prueba Técnica Protección S.A. — Módulo de Aportes Voluntarios  
**Fecha de revisión:** 26 de junio de 2026  
**Revisor:** Experto en revisión de código  
**Tecnologías:** Java 21, Spring Boot 3.4.1, H2, JPA/Hibernate, Lombok  

---

## Resumen ejecutivo

Se encontraron **15 hallazgos** en total:

| Severidad | Cantidad |
|-----------|----------|
| 🔴 Crítica | 2 |
| 🟠 Alta | 4 |
| 🟡 Media | 5 |
| 🔵 Baja | 4 |

A continuación se detalla cada hallazgo ordenado por severidad (de crítica a baja).

---

## 🔴 Hallazgos Críticos

### HC-01 — Inyección SQL en endpoint `/api/aportes/consolidado`

| Campo | Valor |
|-------|-------|
| **Ubicación** | `AporteController.java`, líneas 41–42 |
| **Severidad** | Crítica |

**¿Por qué es un problema?**

La consulta SQL se construye concatenando directamente los parámetros `afiliadoId` y `periodo` sin ningún tipo de saneamiento o parametrización:

```java
String sql = "SELECT * FROM aporte WHERE afiliado_id = '"
        + afiliadoId + "' AND periodo = '" + periodo + "'";
return jdbc.query(sql, aporteRowMapper);
```

Un atacante puede pasar valores como `' OR '1'='1` en `afiliadoId` para extraer todos los registros de la tabla, o combinaciones maliciosas para modificar/escalar la consulta. Dado que la aplicación expone esto como endpoint REST público, el riesgo de explotación es inmediato y severo. Además, el código ya cuenta con `AporteJpaRepository.findByAfiliadoIdAndPeriodo()` que usa consultas parametrizadas (seguras), por lo que el uso de `JdbcTemplate` aquí es completamente innecesario y peligroso.

**¿Cómo lo corregirías?**

Eliminar el uso de `JdbcTemplate` y `RowMapper` del controlador, y delegar la consulta al repositorio JPA o al servicio:

```java
// Opción 1: Usar el repositorio JPA directamente (recomendada)
@GetMapping("/consolidado")
public List<Aporte> consolidado(@RequestParam String afiliadoId,
                                @RequestParam String periodo) {
    return service.obtenerConsolidado(afiliadoId, periodo);
}
```

```java
// En AporteService agregar:
public List<Aporte> obtenerConsolidado(String afiliadoId, String periodo) {
    return aporteRepo.findByAfiliadoIdAndPeriodo(afiliadoId, periodo);
}
```

```java
// Opción 2: Si se requiere JdbcTemplate, usar parámetros named o positional:
String sql = "SELECT * FROM aporte WHERE afiliado_id = ? AND periodo = ?";
return jdbc.query(sql, aporteRowMapper, afiliadoId, periodo);
```

---

### HC-02 — Condición de carrera (race condition) y falta de atomicidad en `registrar()`

| Campo | Valor |
|-------|-------|
| **Ubicación** | `AporteService.java`, líneas 33–67 |
| **Severidad** | Crítica |

**¿Por qué es un problema?**

El método `registrar()` ejecuta un patrón de **lectura-modificación-escritura** (read Saldo → compute nuevo total → save Saldo) **sin `@Transactional`** y **sin bloqueo pesimista u optimista**. Esto implica:

1. Cada operación JPA (`findByAfiliadoId`, `save`) corre en su propia transacción autocommit.
2. Dos hilos concurrentes pueden leer el mismo `totalMes` al mismo tiempo, ambos calcular `nuevo` correctamente, y luego ambos hacer `save`, resultando en **pérdida de una actualización** (lost update).
3. Si `aporteRepo.save(aporte)` falla después de que `saldoRepo.save(s)` ya persistió, el saldo queda actualizado pero el aporte no se registra, dejando los datos en **estado inconsistente**.

En un sistema financiero esto es inaceptable: se pueden perder aportes o duplicar saldos.

**¿Cómo lo corregirías?**

1. Agregar `@Transactional` a nivel de clase o método.
2. Usar `@Lock` pesimista o una versión optimista en la entidad `Saldo`.
3. Persistir primero el `Aporte` y verificar consistencia antes de actualizar el `Saldo`, o hacer ambas operaciones en la misma transacción.

```java
import jakarta.transaction.Transactional;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public Aporte registrar(AporteRequest req) {
    double monto = req.getMonto();
    if (monto <= 0) {
        throw new IllegalArgumentException("El monto debe ser positivo");
    }

    // Bloqueo pesimista para evitar race conditions
    Saldo s = saldoRepo.findByAfiliadoIdWithLock(req.getAfiliadoId())
            .orElseThrow(() -> new IllegalArgumentException("Afiliado no encontrado"));

    double nuevo = s.getTotalMes() + monto;
    if (nuevo > topeMensual) {
        throw new IllegalArgumentException("El monto supera el tope mensual permitido");
    }

    String periodo = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

    Aporte aporte = new Aporte();
    aporte.setAfiliadoId(req.getAfiliadoId());
    aporte.setMonto(monto);
    aporte.setFecha(LocalDate.now());
    aporte.setCanal(req.getCanal());
    aporte.setPeriodo(periodo);
    aporte.setMarcadaRevision(monto > umbralRevision);

    s.setTotalMes(nuevo);
    saldoRepo.save(s);
    eventoRepo.save(new EventoAporte(aporte));
    log.info("Aporte registrado: monto={} afiliado={}", monto, req.getAfiliadoId());

    return aporteRepo.save(aporte);
}
```

En el repositorio `SaldoJpaRepository`:

```java
public interface SaldoJpaRepository extends JpaRepository<Saldo, Long> {
    Optional<Saldo> findByAfiliadoId(String afiliadoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Saldo s WHERE s.afiliadoId = :afiliadoId")
    Optional<Saldo> findByAfiliadoIdWithLock(@Param("afiliadoId") String afiliadoId);
}
```

---

## 🟠 Hallazgos de Alta Severidad

### HA-01 — Lógica de validación del tope mensual incorrecta

| Campo | Valor |
|-------|-------|
| **Ubicación** | `AporteService.java`, líneas 45–47 |
| **Severidad** | Alta |

**¿Por qué es un problema?**

La condición `if (nuevo == topeMensual)` tiene dos defectos graves:

1. **Comparación exacta en vez de umbral**: Se lanza la excepción solo cuando `nuevo` es **exactamente igual** al tope, pero el mensaje dice "supera el tope". Sin embargo, cuando `nuevo` **realmente supera** el tope (`nuevo > topeMensual`), la validación **no falla** y se permite exceder el límite.
2. **Comparación con `==` en `double`**: La igualdad exacta con punto flotante es inherentemente peligrosa. Cálculos como `2_000_000 + 8_000_000` podrían dar `10000000.000000002` por errores de redondeo, evadiendo la validación.

Ejemplo concreto:
- `totalMes = 3_000_000`, `monto = 7_000_000`, `tope = 10_000_000` → `nuevo == topeMensual` → **lanza excepción incorrectamente** (no supera el tope, es igual).
- `totalMes = 3_000_000`, `monto = 8_000_000`, `tope = 10_000_000` → `nuevo = 11_000_000 > topeMensual` → **no lanza excepción**, permitiendo exceder el límite.

**¿Cómo lo corregirías?**

```java
if (nuevo > topeMensual) {
    throw new IllegalArgumentException("El monto supera el tope mensual permitido");
}
```

Adicionalmente, migrar a `BigDecimal` (ver hallazgo HA-02) eliminaría los problemas de precisión.

---

### HA-02 — Uso de `double` para valores monetarios

| Campo | Valor |
|-------|-------|
| **Ubicación** | `Aporte.java:24`, `AporteRequest.java:15`, `Saldo.java:20`, `EventoAporte.java:21`, `AporteService.java:28,30,34,43,45,56,60` |
| **Severidad** | Alta |

**¿Por qué es un problema?**

`double` es un tipo de punto flotante binario (IEEE 754) con precisión limitada. Para operaciones financieras, produce errores de redondeo acumulativos. Por ejemplo, `0.1 + 0.2` no es exactamente `0.3`, y montos como `10_000_000.01` pueden representarse internamente como `10_000_000.009999999`. En un sistema de aportes voluntarios que maneja dinero real, esto puede resultar en:

- Discrepancias en la contabilidad (céntimos perdidos o ganados).
- Validaciones incorrectas como las del hallazgo HA-01.
- Problemas en reportes financieros y auditoría.

**¿Cómo lo corregirías?**

Reemplazar `double` por `BigDecimal` en todas las entidades, DTOs y la lógica del servicio:

```java
// En las entidades y DTOs
private BigDecimal monto;

// En AporteService
private BigDecimal topeMensual;
private BigDecimal umbralRevision;

public Aporte registrar(AporteRequest req) {
    BigDecimal monto = req.getMonto();

    if (monto.compareTo(BigDecimal.ZERO) <= 0) {
        throw new IllegalArgumentException("El monto debe ser positivo");
    }

    // ...

    BigDecimal nuevo = s.getTotalMes().add(monto);

    if (nuevo.compareTo(topeMensual) > 0) {
        throw new IllegalArgumentException("El monto supera el tope mensual permitido");
    }

    s.setTotalMes(nuevo);
    aporte.setMarcadaRevision(monto.compareTo(umbralRevision) > 0);
    // ...
}
```

En `application.properties` ajustar valores con notación BigDecimal-friendly:

```properties
aporte.tope-mensual=10000000.00
aporte.umbral-revision=5000000.00
```

---

### HA-03 — Falta de validación y `@Valid` en el controlador

| Campo | Valor |
|-------|-------|
| **Ubicación** | `AporteController.java:33–36`, `AporteRequest.java` |
| **Severidad** | Alta |

**¿Por qué es un problema?**

El DTO `AporteRequest` no tiene anotaciones de Jakarta Bean Validation (`@NotNull`, `@Positive`, `@NotBlank`), y el controlador no usa `@Valid` en el `@RequestBody`. Esto significa que:

- No hay validación de formato/lógica en la capa de transporte.
- Se pueden enviar `null` en `afiliadoId`, montos negativos o cero, canales vacíos, etc.
- La validación solo ocurre en el servicio (después de atravesar la red y serializar), lo que genera errores 500 con stack traces.

**¿Cómo lo corregirías?**

Agregar anotaciones de validación al DTO y `@Valid` en el controlador:

```java
// AporteRequest.java
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AporteRequest {

    @NotBlank(message = "El afiliadoId es obligatorio")
    private String afiliadoId;

    @Positive(message = "El monto debe ser un valor positivo")
    private double monto;

    @NotBlank(message = "El canal es obligatorio")
    private String canal;
}
```

```java
// AporteController.java
import jakarta.validation.Valid;

@PostMapping
public Aporte registrar(@Valid @RequestBody AporteRequest req) {
    return service.registrar(req);
}
```

También agregar un `@ControllerAdvice` para manejar `MethodArgumentNotValidException` y devolver respuestas HTTP 400 con detalles del error, en vez de 500.

---

### HA-04 — Consulta de `Saldo` no filtra por período mensual

| Campo | Valor |
|-------|-------|
| **Ubicación** | `SaldoJpaRepository.java:10`, `AporteService.java:40–41` |
| **Severidad** | Alta |

**¿Por qué es un problema?**

`findByAfiliadoId()` retorna `Optional<Saldo>` filtrando solo por `afiliadoId`, pero la entidad `Saldo` tiene un campo `mes` (formato `YYYY-MM`). Si un afiliado tiene registros de saldo para varios meses, JPA retorna el primero que encuentre (no determinista), que podría no corresponder al mes actual. La `data.sql` de ejemplo inserta saldos con `mes = '2025-06'`, pero el servicio calcula el período con `LocalDate.now()`. Si la fecha del sistema no coincide, o si se migra a producción con datos históricos, se actualizará el saldo del mes equivocado.

Además, no hay un índice compuesto único `(afiliado_id, mes)` que prevenga duplicados, lo que podría generar múltiples registros de saldo para el mismo afiliado y mes.

**¿Cómo lo corregirías?**

Modificar el repositorio para buscar por `afiliadoId` y `mes`:

```java
// SaldoJpaRepository.java
public interface SaldoJpaRepository extends JpaRepository<Saldo, Long> {
    Optional<Saldo> findByAfiliadoIdAndMes(String afiliadoId, String mes);
}
```

```java
// AporteService.java
String periodo = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
Saldo s = saldoRepo.findByAfiliadoIdAndMes(req.getAfiliadoId(), periodo)
        .orElseThrow(() -> new IllegalArgumentException("Afiliado no encontrado para el período " + periodo));
```

Y en la entidad `Saldo` agregar una constraint única (opcionalmente):

```java
@Entity
@Table(name = "saldo", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"afiliado_id", "mes"})
})
public class Saldo { ... }
```

---

## 🟡 Hallazgos de Severidad Media

### HM-01 — `EventoAporte` depende de `LocalDateTime.now()` en constructor

| Campo | Valor |
|-------|-------|
| **Ubicación** | `EventoAporte.java:27–32` |
| **Severidad** | Media |

**¿Por qué es un problema?**

El constructor `EventoAporte(Aporte aporte)` llama a `LocalDateTime.now()` para fijar la fecha del evento. Esto:

1. Acopla la entidad al reloj del sistema, haciendo difícil las pruebas unitarias (no se puede simular una fecha específica).
2. Viola el principio de separación de responsabilidades: una entidad JPA no debería contener lógica de negocio ni llamadas a servicios de sistema.

**¿Cómo lo corregirías?**

Pasar la fecha como parámetro o establecerla desde el servicio:

```java
// Opción 1: Quitar el constructor y hacerlo en el servicio
EventoAporte evento = new EventoAporte();
evento.setAfiliadoId(aporte.getAfiliadoId());
evento.setMonto(aporte.getMonto());
evento.setTipo("APORTE_REGISTRADO");
evento.setFechaEvento(LocalDateTime.now());

// Opción 2: Constructor con Clock
public EventoAporte(Aporte aporte, LocalDateTime fechaEvento) {
    this.afiliadoId = aporte.getAfiliadoId();
    this.monto = aporte.getMonto();
    this.tipo = "APORTE_REGISTRADO";
    this.fechaEvento = fechaEvento;
}
```

---

### HM-02 — Tests frágiles por fecha fija en `data.sql` y dependencia de `LocalDate.now()`

| Campo | Valor |
|-------|-------|
| **Ubicación** | `data.sql:3-5`, `AporteServiceTest.java`, `AporteService.java:52` |
| **Severidad** | Media |

**¿Por qué es un problema?**

`data.sql` inserta saldos con `mes = '2025-06'`, pero el servicio calcula el período usando `LocalDate.now()`. Los tests fueron escritos en 2025 y funcionaban. Sin embargo, en la fecha actual (26 de junio de 2026), el período que genera el servicio es `'2026-06'` mientras que los saldos en BD tienen `'2025-06'`. La consulta `findByAfiliadoId` (que no filtra por mes, ver HA-04) aún encuentra el registro por casualidad, pero está actualizando un saldo de 2025 cuando debería ser de 2026. Esto genera:

- **Tests que pasan pero con datos inconsistentes** (se actualiza el saldo del año anterior).
- **Fragilidad temporal**: al corregir la consulta (HA-04) los tests fallarán al no encontrar saldo para el mes actual.
- Mala representación de la realidad: los tests no validan el escenario real de persistencia.

**¿Cómo lo corregirías?**

1. Hacer que `data.sql` use un período dinámico o actualizarlo a la fecha actual.
2. Idealmente, usar `@Sql` con configuración por test, o usar `Clock` inyectado en el servicio para poder fijar la fecha en los tests.

```java
// AporteService con Clock inyectable
@Service
public class AporteService {

    private final Clock clock;

    public AporteService(AporteJpaRepository aporteRepo,
                         SaldoJpaRepository saldoRepo,
                         EventoAporteJpaRepository eventoRepo,
                         Clock clock) {
        this.aporteRepo = aporteRepo;
        this.saldoRepo = saldoRepo;
        this.eventoRepo = eventoRepo;
        this.clock = clock;
    }

    private String generarPeriodo() {
        return LocalDate.now(clock).format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }
}

// En el test, pasar un Clock fijo:
@SpringBootTest
class AporteServiceTest {
    @Autowired
    private AporteService service;

    @Test
    void registrar_montoValido_retornaAporte() {
        // Con Clock fijo, data.sql puede tener el mes esperado
        // o usar TestClock para sincronizar
    }
}
```

---

### HM-03 — H2 Console habilitada y sin restricciones de seguridad

| Campo | Valor |
|-------|-------|
| **Ubicación** | `application.properties:14-15` |
| **Severidad** | Media |

**¿Por qué es un problema?**

La consola H2 está habilitada (`enabled=true`) sin ninguna protección (sin `spring.h2.console.settings.web-allow-others=false`). En entornos de desarrollo no es un problema, pero si esta configuración se promociona a entornos superiores (QA, staging, producción), cualquier persona con acceso a la URL `/h2-console` podría ejecutar consultas SQL arbitrarias sobre la base de datos, exponiendo toda la información financiera de los afiliados.

**¿Cómo lo corregirías?**

1. Usar perfiles de Spring para activar la consola solo en desarrollo:

```properties
# application-dev.properties
spring.h2.console.enabled=true

# application.properties (por defecto)
spring.h2.console.enabled=false
```

2. O agregar protección por Spring Security y restringir acceso por IP.

---

### HM-04 — Uso de Lombok `@Data` en entidades JPA

| Campo | Valor |
|-------|-------|
| **Ubicación** | `Aporte.java:12`, `EventoAporte.java:11`, `Saldo.java:9` |
| **Severidad** | Media |

**¿Por qué es un problema?**

`@Data` de Lombok genera `@EqualsAndHashCode` y `@ToString` incluyendo todos los campos, incluido `id`. En entidades JPA:

- `equals()` y `hashCode()` basados en `id` se rompen para objetos no persistidos (todos tienen `id = null`, por lo que todos son iguales).
- `toString()` incluye `id` y puede provocar `LazyInitializationException` si se accede a relaciones lazy durante la serialización.
- Al usar `@Data` + `@AllArgsConstructor` se expone un constructor con todos los campos, lo que puede omitir la lógica de inicialización de la entidad.

**¿Cómo lo corregirías?**

Usar anotaciones más específicas según la necesidad real:

```java
@Entity
@Table(name = "aporte")
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Aporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // otros campos...
}
```

---

### HM-05 — El servicio no distingue entre ausencia de saldo y error del negocio

| Campo | Valor |
|-------|-------|
| **Ubicación** | `AporteService.java:40–41` |
| **Severidad** | Media |

**¿Por qué es un problema?**

Cuando `findByAfiliadoId()` no encuentra un saldo, se lanza `IllegalArgumentException` con el mensaje "Afiliado no encontrado". Sin embargo, es perfectamente válido que un afiliado exista en el sistema pero no tenga un registro de saldo para el mes actual (por ejemplo, si es su primer aporte del mes). Mezclar "afiliado no existe" con "no hay saldo para el período" puede generar confusión en el frontend o en el cliente API, y además no permite crear automáticamente el registro de saldo si es necesario.

**¿Cómo lo corregirías?**

Crear excepciones de negocio personalizadas y permitir la creación automática del saldo si no existe:

```java
// Excepción de negocio
public class AfiliadoNoEncontradoException extends RuntimeException {
    public AfiliadoNoEncontradoException(String afiliadoId) {
        super("Afiliado no encontrado: " + afiliadoId);
    }
}

public class SaldoNoInicializadoException extends RuntimeException {
    public SaldoNoInicializadoException(String afiliadoId, String periodo) {
        super("Saldo no inicializado para afiliado: " + afiliadoId + " período: " + periodo);
    }
}
```

```java
// En AporteService, considerar crear el saldo si no existe:
Saldo s = saldoRepo.findByAfiliadoIdAndMes(req.getAfiliadoId(), periodo)
        .orElseGet(() -> {
            Saldo nuevoSaldo = new Saldo();
            nuevoSaldo.setAfiliadoId(req.getAfiliadoId());
            nuevoSaldo.setMes(periodo);
            nuevoSaldo.setTotalMes(BigDecimal.ZERO);
            return saldoRepo.save(nuevoSaldo);
        });
```

(Esto último depende de la regla de negocio: ¿se permite registrar un aporte si el afiliado no tiene saldo inicializado?)

---

## 🔵 Hallazgos de Severidad Baja

### HB-01 — Mezcla de estilos de persistencia en el controlador

| Campo | Valor |
|-------|-------|
| **Ubicación** | `AporteController.java:19,41-43` |
| **Severidad** | Baja |

**¿Por qué es un problema?**

El controlador inyecta tanto `AporteService` (para registrar) como `JdbcTemplate` (para consultar consolidado). Esto rompe la arquitectura en capas, ya que el controlador no debería tener acceso directo a la base de datos. Si la lógica de consulta cambia (ej: agregar paginación, filtros adicionales), se debe modificar el controlador en lugar del servicio/repositorio.

Además, la existencia de `RowMapper` duplica el mapeo que JPA ya hace automáticamente.

**¿Cómo lo corregirías?**

Mover el endpoint `/consolidado` para usar `AporteService` y el repositorio JPA (como se indicó en HC-01), y eliminar la dependencia de `JdbcTemplate` y `RowMapper` del controlador.

---

### HB-02 — Test carga el contexto completo (`@SpringBootTest`) sin necesidad

| Campo | Valor |
|-------|-------|
| **Ubicación** | `AporteServiceTest.java:12` |
| **Severidad** | Baja |

**¿Por qué es un problema?**

`@SpringBootTest` inicia todo el contexto de Spring (controladores, seguridad, web server, etc.) para probar solo el servicio. Esto hace que los tests sean más lentos de lo necesario y pueden fallar por razones ajenas al servicio (ej: configuración web).

**¿Cómo lo corregirías?**

Usar una anotación más ligera como `@DataJpaTest` + cargar el servicio manualmente, o usar `@SpringBootTest(classes = AporteService.class)` para limitar el contexto. Sin embargo, como el servicio depende de repositorios JPA, la opción más práctica es:

```java
@SpringBootTest(classes = AporteService.class)
class AporteServiceTest { ... }
```

O mejor aún, usar `@ExtendWith(MockitoExtension.class)` para tests unitarios puros con mocks, reservando `@SpringBootTest` para tests de integración.

---

### HB-03 — Tests no usan `@Transactional`, generando efectos colaterales entre sí

| Campo | Valor |
|-------|-------|
| **Ubicación** | `AporteServiceTest.java:12` |
| **Severidad** | Baja |

**¿Por qué es un problema?**

Los tests ejecutan `service.registrar()` que persiste datos en la BD H2 (Saldo, Aporte, EventoAporte). Sin `@Transactional` en cada método, los cambios persisten entre tests. Por ejemplo:

1. `registrar_montoSuperaUmbral_marcaRevision` actualiza el saldo de `AF-002` incrementando su `totalMes`.
2. Si se agrega un test posterior que use `AF-002`, partiría con un saldo diferente al esperado.

Esto hace que los tests sean dependientes del orden de ejecución, una mala práctica.

**¿Cómo lo corregirías?**

Agregar `@Transactional` a nivel de clase de test:

```java
@SpringBootTest
@Transactional
class AporteServiceTest { ... }
```

Cada test revertirá automáticamente los cambios al finalizar.

---

### HB-04 — `spring-boot-starter-jdbc` es innecesario en el `pom.xml`

| Campo | Valor |
|-------|-------|
| **Ubicación** | `pom.xml:36-39` |
| **Severidad** | Baja |

**¿Por qué es un problema?**

La dependencia `spring-boot-starter-jdbc` se agregó para usar `JdbcTemplate` en el controlador. Una vez que se corrija HC-01 (eliminar `JdbcTemplate` del controlador), este starter ya no se necesita, pues `spring-boot-starter-data-jpa` ya incluye JDBC indirectamente. Mantener dependencias no utilizadas aumenta el tiempo de build y el tamaño del artefacto.

**¿Cómo lo corregirías?**

Eliminar la dependencia del `pom.xml`:

```xml
<!-- Eliminar este bloque -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
```

---

## Observaciones Adicionales (No críticas, pero recomendables)

1. **`data.sql` vs schema generation**: La aplicación usa `spring.jpa.hibernate.ddl-auto=create-drop` que genera las tablas automáticamente. El archivo `data.sql` inserta datos en esas tablas. Esto es válido para desarrollo, pero en producción se usarían migraciones (Flyway/Liquibase). Considerar agregar soporte de migraciones para entornos productivos.

2. **Manejo de errores**: No hay un `@ControllerAdvice` global. Si el servicio lanza `IllegalArgumentException`, Spring Boot devuelve un error 500 con un stack trace. Sería mejor mapear estas excepciones a respuestas HTTP 400 con mensajes amigables.

3. **Logging de datos sensibles**: El servicio loggea `monto` y `afiliadoId`. Aunque no son datos extremadamente sensibles (como contraseñas), en producción se debe evaluar si es apropiado loggear información de transacciones financieras, y considerar políticas de retención de logs.

4. **Falta de versión en propiedades del POM**: La propiedad `java.version` está correctamente definida, pero podrían agregarse versiones para otras propiedades como la del plugin de Spring Boot.

---

## Conclusión

El código presenta **2 vulnerabilidades críticas** (inyección SQL y condición de carrera), **4 problemas de alta severidad** (lógica de validación incorrecta, uso de `double` para dinero, falta de validación en controller, consulta de saldo incompleta), y varios problemas de mantenibilidad y buenas prácticas.

Las correcciones prioritarias deben enfocarse en:

1. ❗ Eliminar la inyección SQL y reemplazar `JdbcTemplate` por JPA.
2. ❗ Agregar `@Transactional` y bloqueo pesimista para evitar pérdida de actualizaciones.
3. ❗ Corregir la lógica del tope mensual (`>` en lugar de `==`) y migrar a `BigDecimal`.
4. ❗ Agregar validaciones con Bean Validation y filtrar saldo por período.

Una vez aplicadas estas correcciones, el módulo quedará en condiciones de ser desplegado con seguridad y confiabilidad en un entorno productivo.
