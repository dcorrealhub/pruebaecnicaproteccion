# Auditoría de código — `reto-a`

> **Estado:** cerrada. Todos los archivos del módulo fueron revisados (`controller`, `service`, `domain`, `dto`, `repository`, `test`).

Servicio de registro de aportes a un fondo voluntario. Compilaba y los tests "felices" pasaban, pero el módulo tenía hallazgos que bloquearían un MR en cualquier célula con revisión seria de seguridad y manejo de dinero. Este documento detalla cada hallazgo, su severidad, el código que lo origina y el fix aplicado.

---

## Resumen de hallazgos

| # | Hallazgo | Severidad | Archivo | Estado |
|---|----------|-----------|---------|--------|
| 1 | SQL Injection en `/consolidado` | **Crítico** | `AporteController.java` | ✅ Corregido |
| 2 | `double` para representar dinero | **Crítico** | `Aporte`, `EventoAporte`, `Saldo`, `AporteRequest` | ✅ Corregido |
| 3 | Comparación `==` rompe el control de tope mensual | **Crítico** | `AporteService.java` | ✅ Corregido |
| 4 | Condición de carrera (*lost update*) en `Saldo` | **Alto** | `AporteService.java` | ✅ Corregido |
| 5 | Sin validación de entrada en `AporteRequest` | **Alto** | `AporteRequest.java` | ✅ Corregido |
| 6 | Acceso directo a JDBC desde el controller, bypaseando la capa de servicio | **Medio** | `AporteController.java` | ✅ Corregido |
| 7 | Sin manejo global de excepciones (`IllegalArgumentException` → 500 genérico) | **Medio** | (transversal) | ⚠️ Pendiente |
| 8 | `@Data` de Lombok en entidades JPA | **Bajo** | `Aporte`, `EventoAporte`, `Saldo` | ✅ Corregido |
| 9 | `marcadaRevision` — confirmado, sí tiene uso de negocio | **Informativo** | `Aporte.java` | ✔️ Cerrado, sin acción |
| 10 | Tests usaban literales `double`, no cubrían condición de carrera ni el bug del `==` | **Medio** | `AporteServiceTest.java` | ✅ Corregido (parcial, ver pendientes) |

---

## 1. SQL Injection en `GET /api/aportes/consolidado` — **CRÍTICO**

**Ubicación:** `AporteController.java`, método `consolidado`.

```java
String sql = "SELECT * FROM aporte WHERE afiliado_id = '"
        + afiliadoId + "' AND periodo = '" + periodo + "'";
return jdbc.query(sql, aporteRowMapper);
```

Los parámetros `afiliadoId` y `periodo` llegan desde un `@RequestParam` (entrada de usuario sin sanitizar) y se concatenan directamente dentro de la consulta SQL.

**Ejemplo de explotación:**

```
GET /api/aportes/consolidado?afiliadoId=AF-001' OR '1'='1&periodo=2025-06
```

Esto devuelve **todos** los aportes de **todos** los afiliados, no solo los de `AF-001` — fuga de datos financieros de terceros. Con un motor distinto a H2 en memoria (ej. Postgres en producción), el vector se extiende a exfiltración de cualquier tabla, denegación de servicio, o modificación/borrado de datos según permisos del usuario de BD.

**Por qué pasó:** ya existía `AporteJpaRepository.findByAfiliadoIdAndPeriodo`, parametrizado por diseño, pero el controller lo ignoraba y construía SQL crudo a mano con un `JdbcTemplate` inyectado en paralelo.

**Fix aplicado:** se eliminó el `JdbcTemplate` y el `RowMapper` manual del controller. El endpoint ahora delega en el `service`, que usa el repositorio JPA:

```java
@GetMapping("/consolidado")
public List<Aporte> consolidado(@RequestParam String afiliadoId,
                                 @RequestParam String periodo) {
    return service.consolidado(afiliadoId, periodo);
}
```

```java
// AporteService.java
public List<Aporte> consolidado(String afiliadoId, String periodo) {
    return aporteRepo.findByAfiliadoIdAndPeriodo(afiliadoId, periodo);
}
```

---

## 2. Uso de `double` para representar dinero — **CRÍTICO**

**Ubicación:** `Aporte.monto`, `EventoAporte.monto`, `Saldo.totalMes`, `AporteRequest.monto`, y los `@Value` de tope/umbral en `AporteService`.

`double` es punto flotante binario (IEEE 754) y no puede representar exactamente la mayoría de cantidades decimales:

```java
double a = 0.1;
double b = 0.2;
System.out.println(a + b); // 0.30000000000000004, no 0.3
```

Sumar múltiples aportes mensuales sobre `Saldo.totalMes` puede generar diferencias de centavos que no concilian contra el sistema contable.

**Fix aplicado:** migración completa a `BigDecimal` con escala fija en entidades, DTO, y los parámetros de configuración (`topeMensual`, `umbralRevision`):

```java
@Column(precision = 19, scale = 2)
private BigDecimal monto;
```

```java
@Value("${aporte.tope-mensual:10000000}")
private BigDecimal topeMensual;
```

Toda la aritmética en `AporteService` se reescribió usando los métodos de `BigDecimal` (`.add()`, `.compareTo()`) en vez de operadores (`+`, `==`, `>`), que no aplican a este tipo.

---

## 3. Comparación `==` rompe el control de tope mensual — **CRÍTICO**

**Ubicación:** `AporteService.java`, método `registrar` (código original).

```java
double nuevo = s.getTotalMes() + monto;

if (nuevo == topeMensual) {
    throw new IllegalArgumentException("El monto supera el tope mensual permitido");
}
```

Este es el hallazgo más grave de lógica de negocio del módulo, y no era evidente sin leer `AporteService` completo. El control de tope mensual **no funcionaba en absoluto** para el caso real que debía prevenir:

- La condición usa **igualdad exacta** (`==`) en vez de "mayor o igual que" (`>=`). Si el acumulado supera el tope —por ejemplo `10,000,001` contra un tope de `10,000,000`— la condición es `false` y el aporte se registra sin objeción.
- Solo se bloquea el caso en que el acumulado coincide **exactamente, centavo a centavo**, con el tope configurado — un escenario casi imposible en la práctica dado que los aportes son montos variables.
- Adicionalmente, comparar `double` con `==` es frágil por errores de redondeo de punto flotante, incluso si la lógica de negocio hubiera sido correcta.

**Impacto real:** un afiliado podía superar el tope mensual permitido por el fondo sin que el sistema lo impidiera, ya que la validación nunca se disparaba en la práctica.

**Fix aplicado:**

```java
BigDecimal nuevo = s.getTotalMes().add(monto);

if (nuevo.compareTo(topeMensual) > 0) {
    throw new IllegalArgumentException("El monto supera el tope mensual permitido");
}
```

`compareTo(...) > 0` bloquea correctamente cualquier acumulado que **exceda** el tope, sea por un centavo o por una cantidad mayor — que es la regla de negocio real.

---

## 4. Condición de carrera (*lost update*) en `Saldo` — **ALTO**

**Ubicación:** `AporteService.java`, método `registrar` (código original).

```java
Saldo s = saldoRepo.findByAfiliadoId(req.getAfiliadoId()).orElseThrow(...);
double nuevo = s.getTotalMes() + monto;
s.setTotalMes(nuevo);
saldoRepo.save(s);
```

No había `@Transactional` ni ningún mecanismo de locking. Es un patrón clásico de *read-then-write* sin control de concurrencia. Con dos solicitudes simultáneas de aporte para el mismo afiliado:

1. Ambas leen `s.getTotalMes()` con el mismo valor base (ej. `1,000,000`).
2. Cada una suma su propio `monto` sobre esa misma base.
3. La que escribe último sobrescribe a la otra — un aporte completo se pierde del acumulado de `Saldo`, aunque ambos aportes individuales quedan guardados como filas en `Aporte` (esto generaría una inconsistencia detectable entre el detalle y el consolidado, pero solo después del hecho).

Esto también socavaba aún más el control de tope mensual (ya roto por el hallazgo #3): bajo concurrencia, el chequeo se hacía sobre un valor de `totalMes` potencialmente desactualizado.

**Fix aplicado:** se agregó `@Transactional` al método `registrar`, y un lock pesimista a nivel de fila sobre `Saldo` durante la transacción:

```java
@Transactional
public Aporte registrar(AporteRequest req) {
    // ...
    Saldo s = saldoRepo.findByAfiliadoIdForUpdate(req.getAfiliadoId())
            .orElseThrow(...);
    // ...
}
```

```java
// SaldoJpaRepository.java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT s FROM Saldo s WHERE s.afiliadoId = :afiliadoId")
Optional<Saldo> findByAfiliadoIdForUpdate(@Param("afiliadoId") String afiliadoId);
```

Se eligió lock pesimista en vez de `@Version` (optimistic locking) porque cada registro de aporte necesita el valor *actual* del acumulado para sumar correctamente; un conflicto optimista obligaría a reintentar la transacción completa. Con un lock pesimista, las transacciones concurrentes para el mismo afiliado se serializan de forma simple y predecible. Si el volumen de aportes concurrentes por afiliado creciera significativamente, valdría la pena revisitar con `@Version` + retry para reducir contención.

---

## 5. Sin validación de entrada en `AporteRequest` — **ALTO**

**Ubicación:** `AporteRequest.java` (código original) y `AporteController.registrar`.

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AporteRequest {
    private String afiliadoId;
    private double monto;
    private String canal;
}
```

Sin anotaciones de validación ni `@Valid` en el controller, era posible enviar `monto` negativo o cero (aunque el `service` sí validaba esto en código, no había defensa en la capa HTTP), `afiliadoId` nulo/vacío, o `canal` con cualquier valor arbitrario.

**Fix aplicado:**

```java
public class AporteRequest {

    @NotBlank(message = "afiliadoId es obligatorio")
    private String afiliadoId;

    @NotNull(message = "monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    private BigDecimal monto;

    @NotBlank(message = "canal es obligatorio")
    private String canal;
}
```

```java
@PostMapping
public Aporte registrar(@Valid @RequestBody AporteRequest req) {
    return service.registrar(req);
}
```

> **Nota:** `canal` se deja como `String` con `@NotBlank` en vez de migrarlo a un `enum` (`CanalAporte`) porque no se confirmó con el negocio cuál es el conjunto cerrado de valores válidos (`APP_MOVIL`, `WEB`, ¿otros?). Recomiendo cerrar este punto en un siguiente MR una vez se confirme la lista completa de canales soportados — ver sección de pendientes.

---

## 6. Acceso directo a JDBC desde el controller — **MEDIO**

**Ubicación:** `AporteController.java`.

El controller inyectaba `JdbcTemplate` y construía su propio `RowMapper`, sin pasar por la capa de servicio ni el repositorio JPA — violación de separación de incumbencias (SoC) y duplicación de responsabilidad, dado que `AporteJpaRepository.findByAfiliadoIdAndPeriodo` ya cubría el mismo caso de forma segura.

**Fix aplicado:** resuelto como parte del fix de la sección 1 — se eliminó `JdbcTemplate` y `RowMapper` del controller por completo.

---

## 7. Sin manejo global de excepciones — **MEDIO** ⚠️ Pendiente

**Ubicación:** transversal (no hay `@ExceptionHandler` ni `@ControllerAdvice` en el módulo).

`AporteService.registrar` lanza `IllegalArgumentException` para los casos de monto inválido, afiliado inexistente, y tope mensual excedido. Sin un manejador global, Spring devuelve un **500 Internal Server Error** genérico para estos casos, cuando semánticamente corresponden a un **400 Bad Request** (entrada inválida) o **404 Not Found** (afiliado inexistente).

Esto es un problema real para cualquier cliente del API: no puede distinguir "el servidor falló" de "tu request fue inválido", y expone trazas de stack internas si `server.error.include-stacktrace` no está deshabilitado explícitamente en `application.properties`.

**Fix recomendado (no aplicado aún):**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(Map.of("error", detalle));
    }
}
```

Queda pendiente porque requiere decidir junto al equipo el contrato de error exacto (formato de respuesta, si se distingue 400 de 404 para afiliado inexistente, etc.) — no es una corrección puramente mecánica como las anteriores.

---

## 8. `@Data` de Lombok en entidades JPA — **BAJO**

**Ubicación:** `Aporte.java`, `EventoAporte.java`, `Saldo.java`.

`@Data` genera `equals()`/`hashCode()` sobre todos los campos, lo cual es frágil en entidades JPA: la identidad de un objeto "cambia" cuando pasa de `id = null` a `id` asignado tras persistir, y puede disparar carga perezosa si se agregan relaciones en el futuro.

**Fix aplicado:** se reemplazó `@Data` por `@Getter` + `@Setter` explícitos en las tres entidades.

---

## 9. `marcadaRevision` — confirmado con uso real

**Ubicación:** `Aporte.java`, `AporteService.java`.

```java
aporte.setMarcadaRevision(monto.compareTo(umbralRevision) > 0);
```

Confirmado: no es código muerto. Marca para revisión manual los aportes que superan un umbral de cumplimiento (`umbralRevision`, configurable, default `5,000,000`). Cerrado sin acción.

---

## 10. Tests con literales `double` y cobertura incompleta — **MEDIO**

**Ubicación:** `AporteServiceTest.java` (código original).

```java
var req = new AporteRequest("AF-001", 500_000.0, "APP_MOVIL");
```

Tras migrar `AporteRequest.monto` a `BigDecimal`, estos literales `double` rompían la compilación del test. Más allá de eso, la suite original solo cubría 4 casos felices/de validación básica (monto válido, umbral de revisión, monto negativo, afiliado inexistente) — **no existía ningún test que cubriera el bug del tope mensual (#3) ni la condición de carrera (#4)**, los dos hallazgos más graves de lógica de negocio del módulo.

**Fix aplicado (parcial):** se migraron los 4 tests existentes a `BigDecimal`. **Pendiente** (ver sección final): agregar test de tope mensual excedido y test de concurrencia.

---

## Pendientes para un siguiente MR

- [ ] **Manejo global de excepciones** (`@RestControllerAdvice`) — hallazgo #7, requiere alinear contrato de error con el equipo.
- [ ] **Test de tope mensual:** verificar que un aporte que haga que `totalMes` *supere* (no solo iguale) el tope sea rechazado — cubre directamente la regresión del bug `==` (#3).
- [ ] **Test de concurrencia:** dos hilos registrando aportes simultáneos para el mismo afiliado; verificar que `Saldo.totalMes` refleje la suma de ambos sin pérdidas — cubre el fix de la sección 4.
- [ ] **Definir enum `CanalAporte`** una vez se confirme con negocio el conjunto cerrado de canales válidos (ver nota en hallazgo #5).
- [ ] **Revisar `application.properties` y `data.sql`:** confirmar que la consola H2 (`/h2-console`) no quede expuesta en un perfil de producción, y que no haya credenciales hardcodeadas.

---

## Metodología de revisión

Esta auditoría se hizo leyendo el código real, capa por capa, sin asumir comportamiento no confirmado por el archivo fuente:

1. **Dominio primero** (`Aporte`, `Saldo`, `EventoAporte`): para entender el modelo de datos y detectar de entrada el uso de `double` para dinero.
2. **Controller y DTO**: para mapear los puntos de entrada del sistema (dónde llega input no confiable) y detectar el SQL injection.
3. **Service al final**: porque ahí vive la lógica de negocio real (reglas de tope, concurrencia, efectos secundarios), y es el archivo donde un hallazgo "se confirma o se descarta" — varios hallazgos de este documento quedaron explícitamente marcados como *pendientes de confirmar* hasta tener ese archivo, en vez de especular sobre código no visto.
4. **Tests al cierre**: para verificar si la suite existente realmente protege contra los hallazgos encontrados, o si solo cubre el camino feliz (resultó ser lo segundo: cero cobertura sobre los dos bugs de negocio más graves).

Cada fix propuesto en este documento fue validado contra el código real antes de entregarse, y los cambios se aplicaron de forma incremental verificando que cada capa siguiera compilando contra la anterior (entidad → DTO → service → repositorio → test), en vez de reescribir todo el módulo de una sola vez sin verificación intermedia.

---

---

# Auditoría de construcción — `reto-b`

> **Estado:** implementación completa. 9/9 tests pasan. `BUILD SUCCESS`.

Registro del análisis previo, las decisiones de ingeniería tomadas y cada archivo implementado sobre el scaffold del reto-b (Spring Boot + React). El scaffold ya traía la arquitectura hexagonal definida, los modelos de dominio limpios, las entidades JPA, los repositorios Spring Data y los contratos de los puertos. Los TODOs eran los cuerpos de los métodos: adaptadores, casos de uso, API frontend, y tests.

---

## Qué se analizó antes de escribir una sola línea

### Arquitectura del scaffold

El proyecto sigue **arquitectura hexagonal** (ports & adapters) con separación estricta:

```
domain/
  model/        — Aporte, SaldoMensual, ConsolidadoAportes  (Java puro, sin framework)
  port/
    in/         — RegistrarAporteUseCase, ConsultarAportesUseCase  (interfaces)
    out/        — AporteRepositoryPort, SaldoRepositoryPort          (interfaces)

application/
  usecase/      — RegistrarAporteUseCaseImpl, ConsultarAportesUseCaseImpl  (TODO)

infrastructure/
  persistence/
    entity/     — AporteEntity, SaldoMensualEntity  (JPA)
    repository/ — SpringDataAporteRepository, SpringDataSaldoRepository
    adapter/    — JpaAporteRepositoryAdapter, JpaSaldoRepositoryAdapter  (TODO)
  web/
    AporteController.java  (ya implementado)
    dto/        — RegistrarAporteRequest, AporteResponse, ConsolidadoResponse
```

### Lo que ya venía resuelto (no se tocó)

| Componente | Estado al comenzar |
|---|---|
| `Aporte`, `SaldoMensual`, `ConsolidadoAportes` | Completos, inmutables, sin dependencias de framework |
| `RegistrarAporteUseCase`, `ConsultarAportesUseCase` (interfaces + records) | Completos |
| `AporteRepositoryPort`, `SaldoRepositoryPort` (interfaces) | Completos |
| `AporteEntity`, `SaldoMensualEntity` | Completos con `@Version` para optimistic locking |
| `SpringDataAporteRepository`, `SpringDataSaldoRepository` | Completos |
| `AporteController` | Completo — delega correctamente a los use cases |
| DTOs (`RegistrarAporteRequest`, `AporteResponse`, `ConsolidadoResponse`) | Completos con `@Valid`, `@NotBlank`, `@DecimalMin` |
| Migración Flyway `V1__init.sql` | Completa — tablas `aporte`, `saldo_mensual`, `evento_aporte` con índices e idempotencia |
| `application.properties` | Completo — tope y umbral configurables (`aporte.tope-mensual`, `aporte.umbral-revision`) |
| Componentes React (`RegistrarAporte.jsx`, `ConsolidadoAportes.jsx`) | Completos con estructura, estado y llamadas a la API |

### Lo que faltaba (los TODOs)

Todos los cuerpos de métodos lanzaban `UnsupportedOperationException("Pendiente de implementación")`:

| Archivo | Métodos pendientes |
|---|---|
| `JpaAporteRepositoryAdapter` | `guardar`, `findByIdempotenciaKey`, `findByAfiliadoIdAndPeriodoBetween` |
| `JpaSaldoRepositoryAdapter` | `findByAfiliadoIdAndMes`, `guardar`, `inicializar` |
| `RegistrarAporteUseCaseImpl` | `registrar` |
| `ConsultarAportesUseCaseImpl` | `consultar` |
| `aportesApi.js` | `registrarAporte`, `consultarConsolidado` |

Además, no había ningún `@ControllerAdvice` ni tests de negocio.

---

## Análisis de reglas de negocio antes de implementar

Se leyeron en orden: dominio → puertos → `application.properties` → migración SQL → controller. Resultado:

- **Idempotencia**: la tabla `aporte` tiene `UNIQUE (idempotencia_key)`. La operación debe ser *safe to retry*: si la clave ya existe, retornar el aporte guardado sin duplicar ni modificar el saldo.
- **Monto positivo**: `@DecimalMin(0.01)` lo valida en la capa HTTP, pero el use case debe validarlo también para no depender del transporte.
- **Tope mensual**: configurable via `aporte.tope-mensual` (default 10.000.000). El acumulado del afiliado en el mes **más** el nuevo monto no debe superar ese tope. La lección del reto-a ya estaba presente: se usa `compareTo(topeMensual) > 0`, nunca `==`.
- **Umbral de revisión**: configurable via `aporte.umbral-revision` (default 5.000.000). Si el monto del aporte individual supera ese umbral, el campo `marcada_revision = true`. No bloquea el aporte, solo lo etiqueta.
- **Concurrencia en saldo**: `SaldoMensualEntity` ya tiene `@Version` (optimistic locking). El diseño del scaffold apunta a propagar `OptimisticLockException` en lugar de re-leer el saldo dentro del mismo TX. El use case no atrapa la excepción — la deja subir para que el `@ControllerAdvice` la convierta en 409.
- **Periodo**: se deriva de `LocalDate.now()` en formato `yyyy-MM`. El command no trae fecha — es intencional, el servidor es quien define el momento del registro.

---

## Decisiones de implementación

### 1 — Adaptadores JPA: mapeo en método privado `toX`

Se podría haber usado MapStruct, pero dado el tamaño del modelo (8 campos en `Aporte`, 5 en `SaldoMensual`) el mapeo manual en un método privado `toAporte`/`toSaldo` es más legible y sin dependencia adicional.

```java
// JpaAporteRepositoryAdapter
private Aporte toAporte(AporteEntity e) {
    return new Aporte(e.getId(), e.getAfiliadoId(), e.getMonto(), e.getFecha(),
            e.getCanal(), e.getPeriodo(), e.isMarcadaRevision(), e.getIdempotenciaKey());
}
```

El `id` va como `null` al construir el objeto antes de persistir (`guardar`) y como el valor asignado al leerlo de vuelta. Correcto porque `Aporte` acepta `null` en el constructor y `@GeneratedValue` lo asigna en el `save`.

### 2 — `inicializar` en `JpaSaldoRepositoryAdapter`

Se crea la entidad sin `id` ni `version` (Hibernate/JPA los asigna en `save`). El builder de Lombok sobre `SaldoMensualEntity` acepta `null` para esos campos porque `@Version` arranca en `0` automáticamente en la primera inserción:

```java
SaldoMensualEntity entity = SaldoMensualEntity.builder()
        .afiliadoId(afiliadoId)
        .mes(mes)
        .total(BigDecimal.ZERO)
        .build();
return toSaldo(springDataRepo.save(entity));
```

### 3 — `RegistrarAporteUseCaseImpl`: orden del flujo

El orden importa para no hacer trabajo innecesario ni dejar estado parcial:

1. **Idempotencia primero** — si la clave ya existe, retorno inmediato sin tocar saldo. Ninguna otra validación aplica.
2. **Monto > 0** — falla rápido antes de ir a base de datos.
3. **Cálculo de periodo** — `LocalDate.now()` con formato `yyyy-MM`.
4. **Leer o inicializar saldo** — `orElseGet` llama `inicializar` solo si no existe. La inicialización ocurre dentro del mismo `@Transactional`: si la validación del tope falla justo después, el rollback deshace también la inicialización.
5. **Chequeo de tope** — `calcularNuevoTotal(monto).compareTo(topeMensual) > 0` → `IllegalArgumentException`.
6. **Marca de revisión** — `monto.compareTo(umbralRevision) > 0`. Evalúa el monto individual, no el acumulado.
7. **Guardar saldo** — `guardar(saldo.conTotal(nuevoTotal))`. Si hay conflicto de versión (`OptimisticLockException`), sube sin atrapar.
8. **Persistir aporte** — solo llega aquí si todo lo anterior fue válido.

```java
@Override
@Transactional
public Aporte registrar(RegistrarAporteCommand command) {
    Optional<Aporte> existente = aporteRepository.findByIdempotenciaKey(command.idempotenciaKey());
    if (existente.isPresent()) return existente.get();

    if (command.monto().compareTo(BigDecimal.ZERO) <= 0)
        throw new IllegalArgumentException("El monto debe ser mayor a cero");

    LocalDate fecha = LocalDate.now();
    String periodo = fecha.format(PERIODO_FMT);

    SaldoMensual saldo = saldoRepository.findByAfiliadoIdAndMes(command.afiliadoId(), periodo)
            .orElseGet(() -> saldoRepository.inicializar(command.afiliadoId(), periodo));

    BigDecimal nuevoTotal = saldo.calcularNuevoTotal(command.monto());
    if (nuevoTotal.compareTo(topeMensual) > 0)
        throw new IllegalArgumentException(
                "El aporte supera el tope mensual de " + topeMensual +
                ". Acumulado actual: " + saldo.getTotal());

    boolean marcadaRevision = command.monto().compareTo(umbralRevision) > 0;
    saldoRepository.guardar(saldo.conTotal(nuevoTotal));

    return aporteRepository.guardar(new Aporte(null, command.afiliadoId(), command.monto(),
            fecha, command.canal(), periodo, marcadaRevision, command.idempotenciaKey()));
}
```

### 4 — `ConsultarAportesUseCaseImpl`: `@Transactional(readOnly = true)` y `reduce`

`readOnly = true` permite al driver de PostgreSQL enviar la consulta a una réplica de lectura si el pool está configurado para eso, y evita el *dirty checking* de Hibernate al final del TX (pequeña optimización en colecciones grandes).

La suma usa `reduce` en vez de un `for` mutable — más idiomático con streams y sin posibilidad de `NullPointerException` porque `BigDecimal.ZERO` es el valor identidad garantizado:

```java
BigDecimal total = aportes.stream()
        .map(Aporte::getMonto)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
```

### 5 — `GlobalExceptionHandler`: RFC 9457 `ProblemDetail`

Spring Boot 3.x incluye soporte nativo para `ProblemDetail` (RFC 9457 / Problem Details for HTTP APIs). Se prefirió sobre un `Map<String, String>` genérico porque:

- Es un estándar reconocible por clientes HTTP modernos.
- Evita definir un DTO de error propio.
- Los campos `title` y `detail` son auto-descriptivos.

```java
@ExceptionHandler(IllegalArgumentException.class)
ProblemDetail handleBusiness(IllegalArgumentException ex) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
    pd.setTitle("Regla de negocio violada");
    pd.setDetail(ex.getMessage());
    return pd;
}
```

Se mapearon tres casos:

| Excepción | HTTP | Semántica |
|---|---|---|
| `IllegalArgumentException` | 422 Unprocessable Entity | Regla de negocio violada (monto inválido, tope excedido) |
| `MethodArgumentNotValidException` | 400 Bad Request | Validación de DTO (`@Valid`) — lista de campos inválidos |
| `OptimisticLockException` | 409 Conflict | Conflicto de concurrencia en saldo mensual — el cliente debe reintentar |

> El reto-a tenía el hallazgo #7 pendiente (sin handler global). En reto-b se implementó desde el inicio con formato estándar, cubriendo también el `OptimisticLockException` que la arquitectura del scaffold deja subir intencionalmente.

### 6 — `aportesApi.js`: extracción del mensaje de error

El helper `parseResponse` lee `body.detail` (campo estándar de `ProblemDetail`) antes de `body.message`, lo que hace que el mensaje de negocio llegue limpio al frontend sin exponer detalles del servidor:

```js
async function parseResponse(res) {
  const body = await res.json().catch(() => ({}))
  if (!res.ok) {
    const msg = body.detail || body.message || `Error ${res.status}`
    throw new Error(msg)
  }
  return body
}
```

`consultarConsolidado` usa `URLSearchParams` para construir la query string — evita concatenar strings con riesgo de encoding incorrecto en caracteres especiales del `afiliadoId`.

---

## Tests implementados y qué cubren

Archivo: `RegistrarAporteUseCaseImplTest.java` — 8 tests unitarios con Mockito (`@ExtendWith(MockitoExtension.class)`).

Los campos `@Value` no se inyectan por constructor (no son `final`), así que se inicializan con `ReflectionTestUtils.setField` en `@BeforeEach`.

| Test | Regla de negocio verificada |
|---|---|
| `registrar_idempotente_retorna_aporte_existente_sin_persistir_de_nuevo` | Si `idempotenciaKey` ya existe, retorna el aporte guardado; no llama a `saldoRepository` ni a `aporteRepository.guardar` |
| `registrar_monto_cero_lanza_excepcion` | Monto = 0 → `IllegalArgumentException` con mensaje "mayor a cero" |
| `registrar_monto_negativo_lanza_excepcion` | Monto negativo → `IllegalArgumentException` |
| `registrar_supera_tope_mensual_lanza_excepcion` | Acumulado + nuevo > tope → `IllegalArgumentException` con "tope mensual"; `guardar` nunca se llama |
| `registrar_exactamente_en_tope_mensual_es_valido` | Acumulado + nuevo == tope → aceptado (frontera de la regla) |
| `registrar_monto_sobre_umbral_marca_revision` | Monto > umbral → `marcadaRevision = true` en el aporte retornado |
| `registrar_monto_bajo_umbral_no_marca_revision` | Monto ≤ umbral → `marcadaRevision = false` |
| `registrar_sin_saldo_previo_inicializa_saldo_y_persiste` | Primer aporte del mes → `inicializar` llamado, `guardar(saldo)` recibe el monto correcto, `aporteRepository.guardar` llamado |

El test de la frontera (`exactamente_en_tope_mensual`) es el análogo directo del bug `==` encontrado en reto-a: verifica que el límite sea `>` (estricto) y no `>=` (lo que bloquearía aportes válidos que lleguen justo al tope).

Además corre `RetoBApplicationTest` (context load con H2) — verifica que el contexto de Spring levanta correctamente con todos los beans cableados.

**Resultado:** `Tests run: 9, Failures: 0, Errors: 0, Skipped: 0` — `BUILD SUCCESS`.

---

## Resumen de archivos modificados / creados

| Archivo | Acción | Descripción |
|---|---|---|
| `infrastructure/persistence/adapter/JpaAporteRepositoryAdapter.java` | Implementado | Mapeo `Aporte ↔ AporteEntity`, los 3 métodos del puerto |
| `infrastructure/persistence/adapter/JpaSaldoRepositoryAdapter.java` | Implementado | Mapeo `SaldoMensual ↔ SaldoMensualEntity`, inicialización con `total = 0` |
| `application/usecase/RegistrarAporteUseCaseImpl.java` | Implementado | Idempotencia, validación, tope mensual, marca revisión, `@Transactional` |
| `application/usecase/ConsultarAportesUseCaseImpl.java` | Implementado | Consulta por rango de periodos, suma con `BigDecimal::add`, `@Transactional(readOnly = true)` |
| `infrastructure/web/GlobalExceptionHandler.java` | Creado | `@RestControllerAdvice` con `ProblemDetail` para 3 tipos de excepción |
| `frontend/src/api/aportesApi.js` | Implementado | `registrarAporte` (POST) y `consultarConsolidado` (GET con `URLSearchParams`) |
| `test/.../RegistrarAporteUseCaseImplTest.java` | Creado | 8 tests unitarios de las reglas de negocio del use case |

---

## Pendientes / decisiones conscientemente diferidas

- **Test de integración end-to-end**: verificar el flujo completo contra PostgreSQL real (requiere Docker en CI). Los tests actuales usan mocks en la capa de use case y H2 para el context load. Un test de integración con `@SpringBootTest` + Testcontainers cubriría también la migración Flyway y la unicidad de `idempotencia_key`.
- **`evento_aporte`**: la tabla existe en la migración (`V1__init.sql`) con tipos `APORTE_REGISTRADO` y `APORTE_REVERSADO`, pero no se alimenta en ningún paso de la implementación actual. El scaffold no incluía un port ni un caso de uso para eventos, por lo que se dejó fuera del scope del reto. En producción, publicar el evento dentro del mismo TX del `registrar` daría trazabilidad completa.
- **Retry en `OptimisticLockException`**: actualmente sube como 409. Si el volumen de aportes concurrentes por afiliado justificara reintentos automáticos, se podría envolver `registrar` con `@Retryable(OptimisticLockException.class, maxAttempts = 3)` de Spring Retry sin cambiar la lógica del use case.
- **Enum `Canal`**: `canal` se trata como `String` porque el enunciado solo menciona `APP_MOVIL`, `WEB`, `SUCURSAL` pero no cierra la lista explícitamente. El scaffold optó por `String` en la entidad y el DTO. Convertirlo a `enum` en una siguiente iteración cerraría la validación a nivel de deserialización Jackson.

---

## Metodología de construcción

1. **Leer todo antes de escribir** — se leyeron los 20+ archivos del scaffold antes de tocar un solo TODO. Esto evitó asumir contratos que ya estaban definidos en los puertos o en los DTOs.
2. **Infraestructura primero** — adaptadores JPA antes que use cases. Los use cases dependen de los puertos, no de las implementaciones; pero los tests del contexto Spring levantan toda la cadena, así que necesitaba estar cableada.
3. **Dominio limpio respetado** — no se agregó ninguna anotación de framework ni de Lombok a las clases de `domain/model`. Permanecen Java puro.
4. **Tests desde las reglas, no desde el código** — cada test nació de una regla de negocio del enunciado, no de "cubrir líneas". El test de frontera del tope mensual (`exactamente_en_tope_mensual_es_valido`) surgió directamente del análisis del bug `==` encontrado en reto-a: si allá falló por usar `>=` cuando debía ser `>`, aquí se verifica explícitamente que `>` (y no `>=`) es la semántica correcta.
5. **Verificación final con Maven** — `mvn test` contra la suite completa confirmó 9/9 antes de dar por cerrado el reto.

---

## Historial de commits — reto-b

| Commit | Descripción | Archivos |
|---|---|---|
| `[HUU-6]` | Implementar adaptadores JPA para Aporte y SaldoMensual | `JpaAporteRepositoryAdapter.java`, `JpaSaldoRepositoryAdapter.java` |
| `[HUU-7]` | Implementar RegistrarAporteUseCaseImpl con idempotencia, validación de monto, control de tope mensual, marca de revisión y transacción | `RegistrarAporteUseCaseImpl.java` |
| `[HUU-8]` | Implementar ConsultarAportesUseCaseImpl con consulta por rango de periodos y suma BigDecimal | `ConsultarAportesUseCaseImpl.java` |
| `[HUU-9]` | Agregar GlobalExceptionHandler con ProblemDetail RFC 9457 para errores de negocio, validación y concurrencia | `GlobalExceptionHandler.java` |
| `[HUU-10]` | Implementar aportesApi.js con registrarAporte (POST) y consultarConsolidado (GET) | `aportesApi.js` |
| `[HUU-11]` | Agregar 8 tests unitarios de RegistrarAporteUseCaseImpl cubriendo todas las reglas de negocio | `RegistrarAporteUseCaseImplTest.java` |
| `[HUU-12]` | Documentar construcción reto-b en Auditory.md con análisis, decisiones e historial de commits | `Auditory.md` |