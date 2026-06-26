# Hallazgos de la Auditoría
**Archivos auditados:** `AporteController.java`, `AporteService.java`, `Aporte.java`, `Saldo.java`  

---
## Resumen de Impacto y Riesgos de Negocio
El código cumple su función bajo un "camino feliz", pero presenta riesgos críticos para un entorno productivo y regulado. Si este código se despliega en su estado actual, el sistema se expone a los siguientes riesgos:

* **Fuga de datos y accesos no autorizados (H-01 y H-02):** Cualquier persona con acceso a internet puede descargar el historial financiero de todos los afiliados o registrar aportes fraudulentos, ya que los endpoints carecen de autenticación y son vulnerables a inyección de código.
* **Pérdida de integridad financiera (H-03):** El uso de tipos de datos de punto flotante (`double`) para manejar dinero generará pérdidas de precisión en cada transacción, causando descuadres contables a mediano plazo y haciendo inoperantes las validaciones de topes.
* **Evasión de topes por concurrencia (H-04 y H-05):** Si un usuario envía múltiples peticiones simultáneas (ej. doble clic), el sistema registrará todos los pagos evadiendo los topes máximos mensuales. Adicionalmente, ante un fallo de red o base de datos, el dinero puede alterar el saldo sin dejar rastro transaccional.
* **Deuda técnica y rigidez (H-06):** El diseño acopla la capa de exposición web directamente con la infraestructura de base de datos, dificultando la mantenibilidad futura del módulo.

---
## Metodología de Auditoría
Para garantizar una revisión exhaustiva, se aplicó un enfoque de auditoría asistida por Inteligencia Artificial (Claude), estructurando el análisis de lo general a lo específico, dividiendo el problema en dominios de riesgo técnico.

El proceso consistió en cuatro fases utilizando ingeniería de prompts dirigida:

**Fase 1: Reconocimiento del Sistema**
Se inició el análisis solicitando a la IA que describiera el flujo completo del proyecto y mapeara los puntos críticos de entrada y mutación de datos.

**Fase 2: Auditoría de Seguridad Perimetral**
Se enfocó el análisis en los puntos de entrada para identificar vulnerabilidades de manipulación de datos.

> **Prompt utilizado:**
> *"Actúa como un Auditor de Seguridad experto en Java y Spring Boot. Revisa los archivos AporteController.java y AporteService.java. Tu objetivo principal es encontrar vulnerabilidades de seguridad, prestando especial atención a cómo se manejan los inputs del usuario, cómo se construyen las consultas a la base de datos y la validación de los datos. Documenta cualquier hallazgo crítico indicando el archivo, la línea aproximada, el impacto del riesgo y el código exacto de cómo lo mitigarías."*

**Fase 3: Auditoría Transaccional y Financiera**
Se evaluó el comportamiento del sistema bajo estrés y su precisión en el manejo de dinero.

> **Prompt utilizado:**
> *"Revisa este módulo de Spring Boot asumiendo que corre en un entorno transaccional altamente concurrente. Centra tu análisis exclusivamente en el método registrar de AporteService.java y en los modelos de dominio (Aporte.java y Saldo.java).*
> *1. Evalúa si los tipos de datos utilizados son los correctos para manejar dinero y explica fundamentalmente por qué sí o por qué no.*
> *2. Identifica qué ocurriría si dos hilos de ejecución llaman al método registrar para el mismo afiliadoId en el mismo milisegundo.*
> *3. Identifica riesgos de consistencia de datos ante excepciones (ej. si falla la red en el medio del método).*
> *Propón el código corregido aplicando las mejores prácticas transaccionales."*

**Fase 4: Revisión de Diseño Arquitectónico**
Se analizó el cumplimiento de los límites arquitectónicos para prevenir deuda técnica a futuro.

> **Prompt utilizado:**
> *"Analiza el acoplamiento y la separación de responsabilidades en AporteController.java. Evalúa este código bajo los principios SOLID y los conceptos de Clean Architecture o Arquitectura Hexagonal (puertos y adaptadores). Identifica cualquier violación de los límites arquitectónicos (por ejemplo, capas superiores asumiendo roles de infraestructura). Propón una refactorización de los componentes para que el flujo de lectura (Query) respete la inversión de dependencias."*

---
## Detalle Técnico de Hallazgos
A partir del análisis metodológico, se identificaron **6 hallazgos** principales.
| # | Severidad | Dominio | Título |
|---|-----------|---------|--------|
| H-01 | 🔴 Crítico | Seguridad | SQL Injection en el endpoint de consulta |
| H-02 | 🔴 Crítico | Seguridad | Endpoints sin autenticación ni control de acceso |
| H-03 | 🟠 Alto | Integridad de datos | `double` para aritmética monetaria introduce errores de precisión |
| H-04 | 🟠 Alto | Concurrencia | Race condition: dos aportes simultáneos eluden el tope mensual |
| H-05 | 🟠 Alto | Consistencia | Ausencia de `@Transactional` deja la base de datos en estado inválido |
| H-06 | 🟡 Medio | Arquitectura | Violación de DIP y SRP: infraestructura de base de datos dentro del Controller |

---

## H-01 — SQL Injection

**Archivo:** `AporteController.java` · **Líneas:** 41–43  
**Severidad:** 🔴 Crítico

### Por qué ocurre

Los parámetros `afiliadoId` y `periodo` llegan como strings desde la URL y se pegan directamente dentro de la cadena SQL usando concatenación (`+`). La base de datos recibe ese string completo y lo interpreta como instrucción, sin posibilidad de distinguir dónde terminan los datos y dónde empieza el código.

```java
// Código vulnerable
String sql = "SELECT * FROM aporte WHERE afiliado_id = '"
        + afiliadoId + "' AND periodo = '" + periodo + "'";
return jdbc.query(sql, aporteRowMapper);
```

Con `afiliadoId = ' OR '1'='1` la base de datos ejecuta:
```sql
SELECT * FROM aporte WHERE afiliado_id = '' OR '1'='1' AND periodo = '...'
-- Devuelve todos los aportes de todos los afiliados
```

### Cómo se soluciona

Separar el código SQL de los datos usando parámetros posicionales (`?`). El driver JDBC envía ambas cosas por canales distintos; la base de datos nunca interpreta el valor del parámetro como código.

```java
// Código corregido — AporteController.java
@GetMapping("/consolidado")
public List<Aporte> consolidado(@RequestParam String afiliadoId,
                                @RequestParam String periodo) {
    String sql = "SELECT * FROM aporte WHERE afiliado_id = ? AND periodo = ?";
    return jdbc.query(sql, aporteRowMapper, afiliadoId, periodo);
}
```

> La solución definitiva es mover esta consulta al repositorio JPA (ver H-06), donde Spring Data genera automáticamente sentencias parametrizadas.

---

## H-02 — Endpoints sin autenticación ni control de acceso

**Archivo:** `AporteController.java` · **Líneas:** 33–44  
**Severidad:** 🔴 Crítico

### Por qué ocurre

No existe ningún `SecurityConfig`, filtro, ni anotación de seguridad en el proyecto. Spring Boot sin Spring Security expone todos los endpoints de forma pública por defecto. Cualquier actor en internet puede invocar `POST /api/aportes` para crear aportes fraudulentos, o `GET /api/aportes/consolidado?afiliadoId=X` para leer el historial financiero de cualquier afiliado cuyo ID conozca o adivine.

El segundo problema es de **autorización a nivel de objeto (IDOR/BOLA)**: incluso si se añadiera autenticación, el endpoint de consolidado no verifica que el afiliado autenticado sea el mismo que el `afiliadoId` consultado.

### Cómo se soluciona

**Paso 1 — Agregar Spring Security con JWT stateless:**

```java
// SecurityConfig.java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/aportes/**").authenticated()
                .anyRequest().denyAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .build();
    }
}
```

**Paso 2 — Verificar ownership antes de devolver datos:**

```java
// AporteController.java
@GetMapping("/consolidado")
public List<Aporte> consolidado(@RequestParam String afiliadoId,
                                @RequestParam String periodo,
                                Authentication auth) {
    if (!auth.getName().equals(afiliadoId)) {
        throw new AccessDeniedException("Acceso denegado");
    }
    return service.consolidado(afiliadoId, periodo);
}
```

---

## H-03 — `double` para aritmética monetaria

**Archivos:** `AporteRequest.java:14`, `Saldo.java:20`, `EventoAporte.java:21`, `AporteService.java:27-28`  
**Severidad:** 🟠 Alto

### Por qué ocurre

El tipo `double` implementa el estándar IEEE 754 de aritmética binaria de punto flotante. El problema de fondo es que el sistema decimal (base 10) no tiene representación exacta en binario para la mayoría de fracciones. El número `0.1` se almacena internamente como `0.1000000000000000055511...` porque en base 2 es infinito y periódico, igual que `1/3` en base 10.

Esto produce dos bugs directos en el código:

**Bug A — La comparación `==` nunca se cumple** (`AporteService.java:45`):
```java
double totalMes = 9_500_000.10;   // almacenado: 9500000.0999999962747...
double monto    = 500_000.90;     // almacenado: 500000.9000000000232...
double nuevo    = totalMes + monto; // resultado: 10_000_001.000000000931...

if (nuevo == topeMensual) { ... } // 10_000_001.0000...  == 10_000_000.0  → false ← nunca falla
```

El tope mensual es completamente inoperante: un afiliado puede superar el límite con aportes diseñados para explotar este error.

**Bug B — El saldo acumula error progresivo**: cada `saldoRepo.save()` persiste un valor con ruido flotante. Después de cientos de aportes, la diferencia entre lo que marca la BD y la realidad puede ser de decenas de pesos.

### Cómo se soluciona

`BigDecimal` trabaja en base 10, tiene precisión arbitraria y modos de redondeo deterministas. JPA lo mapea a `NUMERIC(19,2)` en la base de datos.

```java
// Saldo.java
@Column(precision = 19, scale = 2)
private BigDecimal totalMes;

// AporteRequest.java
private BigDecimal monto;

// AporteService.java
@Value("${aporte.tope-mensual:10000000}")
private BigDecimal topeMensual;

@Value("${aporte.umbral-revision:5000000}")
private BigDecimal umbralRevision;

// Comparación correcta en el service
BigDecimal nuevo = s.getTotalMes().add(monto);
if (nuevo.compareTo(topeMensual) >= 0) {   // >= en lugar de ==
    throw new IllegalArgumentException("El monto supera el tope mensual permitido");
}
aporte.setMarcadaRevision(monto.compareTo(umbralRevision) > 0);
```

---

## H-04 — Race Condition: dos aportes simultáneos eluden el tope mensual

**Archivo:** `AporteService.java` · **Líneas:** 40–50  
**Severidad:** 🟠 Alto

### Por qué ocurre

El método `registrar` sigue el patrón **check-then-act**: primero lee el saldo, luego lo compara con el tope, luego lo actualiza. Sin ningún mecanismo de exclusión mutua, dos hilos que ejecuten el mismo método para el mismo `afiliadoId` al mismo tiempo leen **el mismo valor inicial** y ambos pasan el control del tope:

```
Hilo A (aporte 2M)              Hilo B (aporte 2M)         BD: totalMes
──────────────────────────────────────────────────────────────────────────
findByAfiliadoId() → {9M}                                   9M
                                findByAfiliadoId() → {9M}   9M  ← ambos leen 9M
nuevo = 9M + 2M = 11M
11M >= 10M? No  ← BUG (ver H-03)
saldoRepo.save({11M})                                       11M
                                nuevo = 9M + 2M = 11M
                                11M >= 10M? No
                                saldoRepo.save({11M})        11M ← pisa la escritura de A
aporteRepo.save(aporteA)        aporteRepo.save(aporteB)
```

**Resultado:** Existen dos registros `Aporte` de 2M cada uno (4M total), pero el saldo refleja solo 11M en lugar de 13M. Se perdió una escritura (lost update) y se eludió el tope dos veces.

Tomcat tiene 200 hilos por defecto. En producción con carga moderada este escenario es inevitable.

### Cómo se soluciona

Forzar que el acceso al saldo sea **serializado a nivel de base de datos** con `SELECT FOR UPDATE`. Cuando el Hilo A adquiere el lock, el Hilo B queda bloqueado hasta que A haga commit. B entonces lee el saldo ya actualizado (11M) y la validación falla correctamente.

```java
// SaldoJpaRepository.java
public interface SaldoJpaRepository extends JpaRepository<Saldo, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)  // genera SELECT ... FOR UPDATE
    @Query("SELECT s FROM Saldo s WHERE s.afiliadoId = :afiliadoId")
    Optional<Saldo> findByAfiliadoIdForUpdate(@Param("afiliadoId") String afiliadoId);
}

// AporteService.java — usar el método con lock
Saldo s = saldoRepo.findByAfiliadoIdForUpdate(req.getAfiliadoId())
        .orElseThrow(() -> new IllegalArgumentException("Datos del aporte no válidos"));
```

> El pessimistic lock solo funciona dentro de una transacción activa. Este hallazgo y el siguiente (H-05) deben resolverse juntos.

---

## H-05 — Ausencia de `@Transactional`: tres escrituras sin atomicidad

**Archivo:** `AporteService.java` · **Líneas:** 33–66  
**Severidad:** 🟠 Alto

### Por qué ocurre

Sin `@Transactional`, cada llamada a `save()` es una transacción independiente que hace commit inmediatamente. El método ejecuta tres escrituras en secuencia:

```java
saldoRepo.save(s);                        // COMMIT #1 — saldo actualizado en BD
eventoRepo.save(new EventoAporte(aporte)); // COMMIT #2 — si falla aquí...
return aporteRepo.save(aporte);           // COMMIT #3 — este nunca se ejecuta
```

Si ocurre cualquier fallo entre el COMMIT #1 y el COMMIT #3 (excepción de red, reinicio del pod, error de constraint), la base de datos queda en estado inconsistente: **el saldo bajó pero no existe ningún `Aporte` que lo justifique**. Desde el punto de vista de auditoría, desapareció dinero sin trazabilidad.

Hay además un problema de **orden de operaciones**: `eventoRepo.save(new EventoAporte(aporte))` se llama antes de `aporteRepo.save(aporte)`. En ese momento `aporte.getId()` es `null` porque JPA no ha asignado el ID generado por la base de datos. El `EventoAporte` queda sin referencia al `Aporte` real.

### Cómo se soluciona

`@Transactional` envuelve todo el método en una única unidad de trabajo. Spring hace `COMMIT` solo si el método completa sin errores, o `ROLLBACK` automático si lanza cualquier `RuntimeException`. El estado de la base de datos es siempre consistente.

El orden correcto es guardar el `Aporte` primero para obtener el ID generado, y luego crear el evento con la referencia correcta.

```java
// AporteService.java
@Transactional
public Aporte registrar(AporteRequest req) {
    // ... validaciones y cálculo de 'nuevo' ...

    s.setTotalMes(nuevo);
    saldoRepo.save(s);

    Aporte aporte = new Aporte();
    // ... asignar campos ...

    // 1. Guardar primero para obtener el ID generado por la BD
    Aporte persistido = aporteRepo.save(aporte);

    // 2. Crear el evento con el Aporte ya persistido (con ID válido)
    eventoRepo.save(new EventoAporte(persistido));

    log.info("Aporte registrado: monto={} afiliado={}", monto,
             req.getAfiliadoId().replaceAll("[\r\n]", "_")); // sanitizar para log

    return persistido;
}
```

> Con `@Transactional` activo, el `SELECT FOR UPDATE` de H-04 también funciona: el lock se libera al final de la transacción, no al salir de `findByAfiliadoIdForUpdate`.

---

## H-06 — Violación de DIP y SRP: infraestructura dentro del Controller

**Archivo:** `AporteController.java` · **Líneas:** 19–43  
**Severidad:** 🟡 Medio

### Por qué ocurre

El `AporteController` tiene tres responsabilidades que no le pertenecen:

1. **Construir sentencias SQL** (`"SELECT * FROM aporte WHERE ..."`)
2. **Ejecutar consultas contra la base de datos** (vía `JdbcTemplate`)
3. **Mapear `ResultSet` a objetos de dominio** (el `RowMapper` definido inline)

Desde el punto de vista del **Principio de Inversión de Dependencias (DIP)**: el controller es un módulo de alto nivel (adaptador HTTP) que depende directamente de `JdbcTemplate`, un módulo de bajo nivel (detalle de infraestructura JDBC). La dependencia debería apuntar siempre hacia una abstracción, nunca hacia un detalle.

Desde el punto de vista de la **Arquitectura Hexagonal**, el controller es el adaptador primario (lado de entrada); `JdbcTemplate` pertenece al adaptador secundario (lado de salida/base de datos). El código actual los fusiona en la misma clase, rompiendo el límite del hexágono: el flujo de consulta salta directamente de la capa HTTP a la infraestructura sin pasar por ningún puerto de aplicación.

```
Estado actual (incorrecto):

[HTTP]──►[AporteController]──►[JdbcTemplate]──►[DB]
                ↑
         mezcla presentación
         + infraestructura

Estado objetivo (correcto):

[HTTP]──►[AporteController]──►[AporteQueryPort]──►[AporteQueryService]──►[AporteReadRepository]──►[DB]
         (adaptador primario)  (interfaz/puerto)    (aplicación)           (interfaz/puerto)        (adaptador secundario)
```

### Cómo se soluciona

Introducir un **puerto de consulta** (interfaz) que el controller use, y mover toda la lógica de acceso a datos a un adaptador secundario dedicado. El controller nunca importa `JdbcTemplate`.

```java
// 1. Puerto de entrada (interfaz en la capa de aplicación)
public interface AporteQueryPort {
    List<Aporte> consolidado(String afiliadoId, String periodo);
}

// 2. Implementación del puerto (capa de aplicación)
@Service
@RequiredArgsConstructor
public class AporteQueryService implements AporteQueryPort {

    private final AporteReadRepository repository;

    @Override
    public List<Aporte> consolidado(String afiliadoId, String periodo) {
        return repository.findByAfiliadoIdAndPeriodo(afiliadoId, periodo);
    }
}

// 3. Puerto de salida (interfaz de repositorio)
public interface AporteReadRepository {
    List<Aporte> findByAfiliadoIdAndPeriodo(String afiliadoId, String periodo);
}

// 4. Adaptador secundario (infraestructura — el único lugar donde vive JdbcTemplate)
@Repository
@RequiredArgsConstructor
public class AporteJdbcReadAdapter implements AporteReadRepository {

    private final JdbcTemplate jdbc;

    private static final String SQL =
            "SELECT * FROM aporte WHERE afiliado_id = ? AND periodo = ?";

    @Override
    public List<Aporte> findByAfiliadoIdAndPeriodo(String afiliadoId, String periodo) {
        return jdbc.query(SQL, APORTE_ROW_MAPPER, afiliadoId, periodo);
    }

    private static final RowMapper<Aporte> APORTE_ROW_MAPPER = (rs, rowNum) -> {
        Aporte a = new Aporte();
        a.setId(rs.getLong("id"));
        a.setAfiliadoId(rs.getString("afiliado_id"));
        a.setMonto(rs.getBigDecimal("monto"));
        a.setFecha(rs.getDate("fecha").toLocalDate());
        a.setCanal(rs.getString("canal"));
        a.setPeriodo(rs.getString("periodo"));
        a.setMarcadaRevision(rs.getBoolean("marcada_revision"));
        return a;
    };
}

// 5. Controller limpio: solo recibe HTTP y delega al puerto
@RestController
@RequestMapping("/api/aportes")
@RequiredArgsConstructor
public class AporteController {

    private final AporteService service;
    private final AporteQueryPort query;    // depende de interfaz, no de JdbcTemplate

    @PostMapping
    public Aporte registrar(@Valid @RequestBody AporteRequest req) {
        return service.registrar(req);
    }

    @GetMapping("/consolidado")
    public List<Aporte> consolidado(@RequestParam String afiliadoId,
                                    @RequestParam String periodo,
                                    Authentication auth) {
        if (!auth.getName().equals(afiliadoId)) {
            throw new AccessDeniedException("Acceso denegado");
        }
        return query.consolidado(afiliadoId, periodo);
    }
}
```

Con esta estructura, cambiar el mecanismo de consulta (de JDBC a JPA, a un caché, o a un microservicio) solo requiere crear una nueva implementación de `AporteReadRepository`. El controller y el service no cambian.

---