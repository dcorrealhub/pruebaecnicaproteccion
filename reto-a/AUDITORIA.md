# Auditoría de Código — Módulo `reto-a` (Aportes Voluntarios)

**Rol:** Principal Software Engineer / Auditor Senior — CIS Protección S.A.
**Marco:** Clean Architecture · SOLID · CQRS · DevSecOps · Regulación SFC
**Fecha:** 2026-06-26

---

## Mapa de Componentes

| Componente | Tipo | Responsabilidad |
|---|---|---|
| `RetoAApplication.java` | Entrypoint | Arranca el contexto Spring Boot; no contiene lógica propia |
| `AporteController.java` | Controller | Expone `POST /api/aportes` y `GET /api/aportes/consolidado`; contiene ilegalmente `JdbcTemplate` y SQL directo |
| `AporteRequest.java` | DTO de entrada | Contrato de la solicitud: `afiliadoId`, `monto` (double), `canal` — sin ninguna validación declarativa |
| `AporteService.java` | Service | Lógica de registro: valida tope, actualiza saldo, persiste aporte y evento — sin `@Transactional` |
| `Aporte.java` | Entidad JPA | Agregado principal del aporte; usa `double` para monto, sin llave de idempotencia |
| `EventoAporte.java` | Entidad JPA | Registro de evento/audit trail; se acopla a `Aporte` vía constructor de conversión y se persiste antes de que el aporte tenga ID |
| `Saldo.java` | Entidad JPA | Acumulado mensual del afiliado; sin `@Version` ni locking, blanco directo de condición de carrera |
| `AporteJpaRepository.java` | Repositorio JPA | CRUD + `findByAfiliadoIdAndPeriodo` — este método existe pero **no se usa** en el `GET` del controller |
| `EventoAporteJpaRepository.java` | Repositorio JPA | Solo `save` de eventos; sin queries propias ni operaciones de lectura |
| `SaldoJpaRepository.java` | Repositorio JPA | `findByAfiliadoId` sin locking declarativo; expone el saldo a race conditions |
| `application.properties` | Configuración | H2 en memoria, JPA, puerto 8080; consola H2 habilitada globalmente sin scoping por perfil |
| `data.sql` | Fixture de datos | Inicializa tres saldos sintéticos (`AF-001`, `AF-002`, `AF-003`) que los tests consumen con estado compartido |
| `AporteServiceTest.java` | Tests | 4 pruebas `@SpringBootTest` sin `@Transactional`; estado compartido hace los tests dependientes del orden de ejecución |

---

## Hallazgos

---

### Hallazgo N° 1: Inyección SQL en el endpoint `GET /consolidado`

*   **Ubicación:** `AporteController.java`, método `consolidado()`, líneas 40–43
*   **Severidad:** Crítica
*   **Por qué es un problema:**
    La consulta se construye concatenando directamente los parámetros de la request en un string SQL sin ningún saneamiento:
    ```java
    String sql = "SELECT * FROM aporte WHERE afiliado_id = '"
            + afiliadoId + "' AND periodo = '" + periodo + "'";
    return jdbc.query(sql, aporteRowMapper);
    ```
    Un atacante puede enviar `afiliadoId = ' OR '1'='1` para exfiltrar los aportes de **todos** los afiliados del fondo, o `'; DROP TABLE aporte; --` para destruir la tabla completa. En un entorno regulado por la SFC (Circular Externa 007/2018 y Circular Básica Jurídica), la exfiltración de información financiera de afiliados constituye una violación grave con consecuencias legales y sanciones administrativas para Protección S.A. Esta es la vulnerabilidad de mayor severidad del módulo.

*   **Cómo lo corregirías:**
    Corrección inmediata: usar parámetros posicionales de `JdbcTemplate`. Corrección arquitectónica definitiva: eliminar `JdbcTemplate` del controller completamente y delegar al repositorio JPA existente (ver Hallazgo N° 7):
    ```java
    // Solución inmediata — si el JdbcTemplate se mantiene temporalmente:
    String sql = "SELECT * FROM aporte WHERE afiliado_id = ? AND periodo = ?";
    return jdbc.query(sql, aporteRowMapper, afiliadoId, periodo);

    // Solución correcta — usar el repositorio que ya existe:
    // AporteJpaRepository.findByAfiliadoIdAndPeriodo(afiliadoId, periodo)
    ```

---

### Hallazgo N° 2: Uso de `double` para representar valores monetarios en pesos colombianos

*   **Ubicación:**
    - `AporteRequest.java:13` — `private double monto;`
    - `Aporte.java:24` — `private double monto;`
    - `Saldo.java:20` — `private double totalMes;`
    - `EventoAporte.java:21` — `private double monto;`
    - `AporteService.java:34` — variable local `double monto`
    - `AporteService.java:36,37` — `@Value` bound a `double topeMensual` y `double umbralRevision`
*   **Severidad:** Crítica
*   **Por qué es un problema:**
    El estándar IEEE 754 de punto flotante (`double`) no puede representar exactamente la mayoría de los valores decimales. En aritmética financiera:
    ```
    4_999_999.99 + 0.01  →  5_000_000.0000000005  (desbordamiento silencioso)
    0.1 + 0.2            →  0.30000000000000004    (nunca igual a 0.30)
    ```
    Con las 14.000–18.000 transacciones mensuales del sistema, el error acumulado puede superar decenas de miles de pesos por periodo. Esto genera inconsistencias en los libros contables, reportes incorrectos a la SFC en cierres de mes, y potencial litigio con afiliados sobre el valor real de sus aportes y beneficios tributarios.

*   **Cómo lo corregirías:**
    Reemplazar **todos** los `double`/`float` monetarios por `BigDecimal` con escala de 2 y modo `HALF_UP` en toda la cadena:
    ```java
    // Aporte.java
    @Column(precision = 19, scale = 2)
    private BigDecimal monto;

    // Saldo.java
    @Column(precision = 19, scale = 2)
    private BigDecimal totalMes;

    // AporteService.java
    @Value("${aporte.tope-mensual:10000000}")
    private BigDecimal topeMensual;

    BigDecimal nuevo = s.getTotalMes().add(monto);
    if (nuevo.compareTo(topeMensual) > 0) { ... }
    ```
    En el DDL de H2/SQL Server, declarar las columnas como `NUMERIC(19,2)`.

---

### Hallazgo N° 3: Condición de carrera en la actualización del saldo mensual

*   **Ubicación:** `AporteService.java`, método `registrar()`, líneas 40–50; `SaldoJpaRepository.java`; `Saldo.java`
*   **Severidad:** Crítica
*   **Por qué es un problema:**
    El flujo de actualización de saldo sigue el patrón read-modify-write sin ningún mecanismo de exclusión mutua:
    ```java
    Saldo s = saldoRepo.findByAfiliadoId(...).orElseThrow(...);  // READ
    double nuevo = s.getTotalMes() + monto;                       // MODIFY en memoria
    s.setTotalMes(nuevo);
    saldoRepo.save(s);                                            // WRITE
    ```
    Dos solicitudes concurrentes para el mismo afiliado pueden leer el mismo saldo base (e.g., `0`) antes de que cualquiera complete el `save`. Ambas calculan `nuevo = 5.000.000` y ambas persisten ese valor. El acumulado real debería ser `10.000.000`, pero queda en `5.000.000`. El tope mensual queda completamente inoperante bajo carga concurrente. Esta condición es explotable deliberadamente para registrar aportes ilimitados en el fondo.

*   **Cómo lo corregirías:**

    **Opción A — Locking optimista** (preferida para alta concurrencia, menor contención):
    ```java
    // Saldo.java
    @Version
    private Long version;  // JPA lanza OptimisticLockException en conflicto → reintentar
    ```

    **Opción B — Locking pesimista** (más simple):
    ```java
    // SaldoJpaRepository.java
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Saldo s WHERE s.afiliadoId = :afiliadoId")
    Optional<Saldo> findByAfiliadoIdForUpdate(@Param("afiliadoId") String afiliadoId);
    ```

    **Opción C — UPDATE atómico** (elimina el read-modify-write del todo, más performante):
    ```java
    @Modifying
    @Query("""
        UPDATE Saldo s SET s.totalMes = s.totalMes + :monto
        WHERE s.afiliadoId = :afiliadoId
          AND (s.totalMes + :monto) <= :tope
        """)
    int incrementarSaldo(String afiliadoId, BigDecimal monto, BigDecimal tope);
    // retorna 0 si el tope fue excedido → lanzar excepción de dominio
    ```

---

### Hallazgo N° 4: Error lógico crítico en la validación del tope mensual — `==` en lugar de `>`

*   **Ubicación:** `AporteService.java`, línea 45
*   **Severidad:** Crítica
*   **Por qué es un problema:**
    ```java
    if (nuevo == topeMensual) {   // solo rechaza si nuevo es EXACTAMENTE igual al tope
        throw new IllegalArgumentException("El monto supera el tope mensual permitido");
    }
    ```
    La condición con `==` solo dispara cuando el nuevo total es **exactamente igual** a 10.000.000. Cualquier valor que lo supere (10.000.001, 20.000.000, 100.000.000) pasa **sin ninguna restricción**. Agravante: comparar `double` con `==` es inherentemente impreciso por representación IEEE 754 — `9_500_000.0 + 500_000.0` no es garantizadamente `10_000_000.0` exacto, por lo que el tope tampoco se dispara en el único caso que debería. El resultado es que el límite mensual es letra muerta en producción.

*   **Cómo lo corregirías:**
    Con `BigDecimal` como prerrequisito (Hallazgo N° 2):
    ```java
    BigDecimal nuevo = s.getTotalMes().add(monto);
    if (nuevo.compareTo(topeMensual) > 0) {
        throw new TopeMensualExcedidoException(
            "Tope mensual de " + topeMensual + " excedido. " +
            "Saldo actual: " + s.getTotalMes() + ", aporte solicitado: " + monto);
    }
    ```

---

### Hallazgo N° 5: Ausencia de llave de idempotencia — aportes duplicados ante reintentos de red

*   **Ubicación:** `AporteController.java`, `AporteRequest.java`, `AporteService.java`, `Aporte.java`
*   **Severidad:** Crítica
*   **Por qué es un problema:**
    El endpoint `POST /api/aportes` no tiene ningún mecanismo de idempotencia. Un timeout de red, un error 5xx o un reintento automático del cliente resultan en múltiples aportes idénticos registrados: el saldo se debita N veces por la misma operación. En un fondo voluntario de pensiones, la doble contabilización de un aporte puede generar beneficios tributarios incorrectos (Artículo 126-1 del Estatuto Tributario) y es una violación directa de los principios de integridad contable exigidos por la SFC.

*   **Cómo lo corregirías:**
    1. Agregar `idempotencyKey` (UUID v4 generado por el cliente) a `AporteRequest`:
    ```java
    @NotBlank
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89ab][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$")
    private String idempotencyKey;
    ```
    2. Agregar columna `idempotency_key VARCHAR(36) UNIQUE NOT NULL` a la tabla `aporte`.
    3. En `AporteService.registrar()`, verificar antes de procesar:
    ```java
    aporteRepo.findByIdempotencyKey(req.getIdempotencyKey())
        .ifPresent(existing -> { throw new AporteDuplicadoException(existing); });
    ```
    4. Ante `AporteDuplicadoException`, retornar el aporte original con HTTP 200 (idempotente).

---

### Hallazgo N° 6: Ausencia de delimitador transaccional — inconsistencia entre tablas ante fallo parcial

*   **Ubicación:** `AporteService.java`, método `registrar()`, líneas 33–67
*   **Severidad:** Alta
*   **Por qué es un problema:**
    El método realiza cuatro escrituras en tres tablas distintas **sin** `@Transactional`:
    1. `saldoRepo.save(s)` — saldo incrementado y commiteado de forma independiente
    2. `eventoRepo.save(new EventoAporte(aporte))` — evento persistido
    3. `aporteRepo.save(aporte)` — aporte principal

    Si la operación 3 falla (constraint violation, timeout de DB), el saldo ya fue incrementado permanentemente pero no existe ningún `Aporte` que lo justifique. El sistema queda en un estado inconsistente que no puede reconciliarse automáticamente: el afiliado "gastó" cupo de su tope mensual sin que el aporte exista en los registros.

*   **Cómo lo corregirías:**
    ```java
    @Transactional(rollbackFor = Exception.class)
    public Aporte registrar(AporteRequest req) { ... }
    ```
    Reordenar las operaciones: persistir el `Aporte` antes del `EventoAporte` para que el evento siempre referencia un aporte con ID asignado (ver también Hallazgo N° 14).

---

### Hallazgo N° 7: Violación de Clean Architecture — `JdbcTemplate` y `RowMapper` en la capa de transporte

*   **Ubicación:** `AporteController.java`, campos `jdbc` y `aporteRowMapper`, líneas 15–30 y 40–43
*   **Severidad:** Alta
*   **Por qué es un problema:**
    El controller inyecta `JdbcTemplate` directamente y define inline un `RowMapper<Aporte>` que mapea `ResultSet` a entidades de dominio. La capa de transporte HTTP no debe conocer SQL, mapeos de filas ni detalles de infraestructura de persistencia. Esto viola la regla de dependencias de Clean Architecture: las capas externas no deben importar artefactos de capas internas de infraestructura. Adicionalmente, `AporteJpaRepository.findByAfiliadoIdAndPeriodo()` ya implementa exactamente esta consulta — el SQL ad-hoc crea un segundo camino de lectura divergente que puede retornar resultados distintos si la consulta JPA evoluciona.

*   **Cómo lo corregirías:**
    Eliminar `JdbcTemplate`, `RowMapper` y el SQL del controller. Delegar al repositorio y al servicio:
    ```java
    // AporteController.java — solo responsabilidad HTTP
    @GetMapping("/consolidado")
    public List<AporteResponse> consolidado(
            @RequestParam @NotBlank String afiliadoId,
            @RequestParam @NotBlank String periodo) {
        return queryService.consolidado(afiliadoId, periodo);
    }

    // AporteQueryService.java
    @Transactional(readOnly = true)
    public List<AporteResponse> consolidado(String afiliadoId, String periodo) {
        return aporteRepo.findByAfiliadoIdAndPeriodo(afiliadoId, periodo)
                         .stream().map(AporteMapper::toResponse).toList();
    }
    ```

---

### Hallazgo N° 8: Violación de CQRS — comando y consulta sin separación de contratos ni servicios

*   **Ubicación:** `AporteService.java` — un solo servicio con mezcla de escritura y lectura; `AporteController.java`
*   **Severidad:** Alta
*   **Por qué es un problema:**
    El marco CIS exige separación Comando/Consulta (CQRS). El comando `registrar` (escritura con efectos de lado: incremento de saldo, creación de evento) y la consulta `consolidado` (lectura pura) viven en el mismo servicio sin separación de contratos. Esto impide: (a) aplicar `readOnly = true` a las transacciones de lectura para optimización de DB, (b) escalar el read path de forma independiente, (c) auditar por separado escrituras y lecturas en el log de operaciones.

*   **Cómo lo corregirías:**
    Separar en dos servicios con contratos distintos:
    ```
    AporteCommandService  →  registrar()   — @Transactional(rollbackFor = Exception.class)
    AporteQueryService    →  consolidado() — @Transactional(readOnly = true)
    ```
    El controller inyecta `AporteCommandService` para `POST` y `AporteQueryService` para `GET`.

---

### Hallazgo N° 9: Exposición de datos sensibles de afiliados en mensajes de excepción

*   **Ubicación:** `AporteService.java`, línea 41
*   **Severidad:** Alta
*   **Por qué es un problema:**
    ```java
    .orElseThrow(() -> new IllegalArgumentException(
        "Afiliado no encontrado: " + req.getAfiliadoId()));
    ```
    El `afiliadoId` se incluye en el mensaje de la excepción. Sin un `@ControllerAdvice` configurado (que no existe en este módulo), Spring Boot expone el mensaje completo de `IllegalArgumentException` en el body de la respuesta 500. Esto revela la estructura y formato de los identificadores internos de afiliados a cualquier cliente que reciba el error. Bajo la Ley 1581 de 2012 (Protección de Datos Personales) y las circulares de la SFC sobre seguridad de la información, los identificadores de afiliados de fondos de pensiones son datos personales sensibles con protección especial.

*   **Cómo lo corregirías:**
    Mensaje genérico al cliente; detalle solo al log interno con nivel `WARN`:
    ```java
    .orElseThrow(() -> {
        log.warn("Solicitud de aporte para afiliado inexistente. afiliadoId={}", req.getAfiliadoId());
        return new AfiliadoNotFoundException(
            "El afiliado indicado no existe o no está habilitado para realizar aportes");
    });
    ```

---

### Hallazgo N° 10: Registro de datos financieros sensibles en logs de aplicación en texto plano

*   **Ubicación:** `AporteService.java`, línea 64
*   **Severidad:** Alta
*   **Por qué es un problema:**
    ```java
    log.info("Aporte registrado: monto={} afiliado={}", monto, req.getAfiliadoId());
    ```
    El monto exacto del aporte y el identificador del afiliado quedan en texto plano en los logs de aplicación al nivel `INFO`. Los logs de aplicación en entornos productivos son accedidos por equipos de operaciones, SRE y plataforma que no deberían tener visibilidad sobre transacciones financieras individuales de afiliados. La Circular SFC sobre requerimientos mínimos de seguridad exige controles de acceso diferenciados para datos de transacciones de clientes. Este log los expone a cualquier operador con acceso a los ficheros de log o a la plataforma de observabilidad.

*   **Cómo lo corregirías:**
    Separar niveles de sensibilidad: `INFO` solo publica el ID del aporte y metadatos no financieros; el monto queda en `DEBUG` (deshabilitado en producción) o en un log de auditoría estructurado con acceso controlado (RBAC):
    ```java
    log.info("Aporte registrado. aporteId={} periodo={} canal={} marcadaRevision={}",
             aporte.getId(), aporte.getPeriodo(), aporte.getCanal(), aporte.isMarcadaRevision());
    log.debug("Detalle financiero aporte. afiliadoId={} monto={}", req.getAfiliadoId(), monto);
    ```

---

### Hallazgo N° 11: Ausencia de Bean Validation en el DTO de entrada y `@Valid` en el controller

*   **Ubicación:** `AporteRequest.java` — sin anotaciones JSR-380; `AporteController.java:34` — `@RequestBody AporteRequest req` sin `@Valid`
*   **Severidad:** Media
*   **Por qué es un problema:**
    La validación de entrada se realiza manualmente en la capa de servicio con `if`/`throw`, en lugar de declarativamente en el boundary de entrada. Consecuencias: (a) un `afiliadoId` nulo llega hasta el repositorio provocando una `NullPointerException` no controlada; (b) un `canal` nulo se persiste en DB sin error; (c) los mensajes de error son inconsistentes; (d) la lógica de validación queda acoplada al servicio en lugar de vivir en el contrato del DTO.

*   **Cómo lo corregirías:**
    Agregar `spring-boot-starter-validation` al `pom.xml` y anotar el DTO:
    ```java
    @NotBlank(message = "El afiliadoId es requerido")
    @Size(max = 20)
    @Pattern(regexp = "^[A-Z0-9\\-]+$")
    private String afiliadoId;

    @NotNull
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    private BigDecimal monto;

    @NotBlank
    @Pattern(regexp = "^(APP_MOVIL|WEB|PRESENCIAL|ATM)$", message = "Canal inválido")
    private String canal;

    // Controller:
    public Aporte registrar(@Valid @RequestBody AporteRequest req) { ... }
    ```

---

### Hallazgo N° 12: Jerarquía de excepciones inadecuada — `IllegalArgumentException` para todos los errores de dominio

*   **Ubicación:** `AporteService.java`, líneas 36, 41, 45
*   **Severidad:** Media
*   **Por qué es un problema:**
    Las tres condiciones de error de negocio distintas lanzan la misma `IllegalArgumentException` de la JDK, sin semántica de dominio. Sin un `@RestControllerAdvice`, Spring Boot mapea todas a HTTP 500. El cliente externo no puede diferenciar "afiliado inexistente" (debería ser 404) de "tope mensual excedido" (debería ser 422) de "monto inválido" (400), porque todas retornan el mismo código de estado y mensaje genérico.

*   **Cómo lo corregirías:**
    Definir una jerarquía bajo `co.proteccion.cis.retoa.exception` y un `@RestControllerAdvice` que las mapee:
    ```
    AfiliadoNotFoundException      → HTTP 404
    TopeMensualExcedidoException   → HTTP 422
    MontoInvalidoException         → HTTP 400
    AporteDuplicadoException       → HTTP 409
    ```

---

### Hallazgo N° 13: Consola H2 habilitada globalmente sin scoping por perfil de despliegue

*   **Ubicación:** `application.properties`, líneas 13–15
*   **Severidad:** Media
*   **Por qué es un problema:**
    ```properties
    spring.h2.console.enabled=true
    spring.h2.console.path=/h2-console
    ```
    Esta configuración aplica a **todos** los perfiles al no estar en `application-dev.properties`. En ausencia de Spring Security (que no está configurado en este módulo), la consola H2 permite ejecutar SQL arbitrario contra la base de datos sin autenticación. Si este `application.properties` se despliega en un entorno no-productivo con acceso de red, la superficie de ataque es total: cualquier actor con acceso al puerto puede leer o modificar todos los datos.

*   **Cómo lo corregirías:**
    Deshabilitar por defecto en el archivo base y habilitar solo en el perfil de desarrollo:
    ```properties
    # application.properties (base — todos los entornos)
    spring.h2.console.enabled=false

    # application-dev.properties (solo desarrollo local)
    spring.h2.console.enabled=true
    spring.h2.console.path=/h2-console
    ```

---

### Hallazgo N° 14: Acoplamiento estructural entre entidades de dominio — constructor `EventoAporte(Aporte)` y orden de persistencia invertido

*   **Ubicación:** `EventoAporte.java`, líneas 26–31; `AporteService.java`, línea 62
*   **Severidad:** Media
*   **Por qué es un problema:**
    Dos problemas encadenados:

    **1. Acoplamiento estructural:** `EventoAporte` conoce la estructura interna de `Aporte` vía un constructor de conversión directo. Si `Aporte` evoluciona (campo renombrado, eliminado), el compilador no detecta el error — el evento queda con datos obsoletos o nulos silenciosamente.

    **2. Orden de persistencia invertido:** el evento se persiste **antes** que el aporte:
    ```java
    eventoRepo.save(new EventoAporte(aporte));  // aporte.getId() = null aquí
    return aporteRepo.save(aporte);             // ID asignado aquí — demasiado tarde
    ```
    El `EventoAporte` existe en DB sin poder referenciar el `id` del aporte que lo originó, rompiendo la trazabilidad del audit trail.

*   **Cómo lo corregirías:**
    Invertir el orden y usar un factory method en lugar del constructor de conversión:
    ```java
    Aporte saved = aporteRepo.save(aporte);           // ID asignado
    eventoRepo.save(EventoAporte.from(saved));         // evento con aporteId = saved.getId()
    ```

---

### Hallazgo N° 15: Tests con estado compartido y dependencia implícita del orden de ejecución

*   **Ubicación:** `AporteServiceTest.java`; `data.sql`
*   **Severidad:** Baja
*   **Por qué es un problema:**
    Los cuatro tests usan `@SpringBootTest` sobre una única instancia H2 con el estado inicial de `data.sql`. Los tests `registrar_montoValido_retornaAporte` y `registrar_montoSuperaUmbral_marcaRevision` modifican los saldos de `AF-001` y `AF-002` permanentemente. JUnit 5 no garantiza orden de ejecución por defecto: si `registrar_montoSuperaUmbral_marcaRevision` corre después de otro test que ya sumó al saldo de `AF-002`, el saldo puede superar el tope y el test fallará de forma opaca, no por un bug real sino por acoplamiento de estado.

*   **Cómo lo corregirías:**
    Anotar la clase de test con `@Transactional` para que cada test revierta sus cambios automáticamente. Alternativamente:
    ```java
    @Sql(scripts = "/data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    ```
    O aislar por test usando identificadores únicos por caso: `"AF-TEST-VALIDO-" + UUID.randomUUID()`.

---

## Resumen Ejecutivo

| N° | Hallazgo | Severidad | Vector |
|---|---|---|---|
| 1 | SQL Injection en `GET /consolidado` | **Crítica** | Seguridad |
| 2 | `double` para valores monetarios | **Crítica** | Precisión numérica |
| 3 | Condición de carrera en saldo mensual | **Crítica** | Concurrencia / Idempotencia |
| 4 | `==` en validación de tope (lógica rota) | **Crítica** | Corrección de negocio |
| 5 | Sin llave de idempotencia — aportes duplicados | **Crítica** | Idempotencia |
| 6 | Sin `@Transactional` — inconsistencia entre tablas | **Alta** | Arquitectura / Consistencia |
| 7 | `JdbcTemplate` + SQL en el Controller | **Alta** | Clean Architecture |
| 8 | Sin separación Comando/Consulta | **Alta** | CQRS |
| 9 | `afiliadoId` en mensaje de excepción expuesto al cliente | **Alta** | Seguridad / SFC |
| 10 | Monto financiero en log `INFO` texto plano | **Alta** | Seguridad / SFC |
| 11 | Sin Bean Validation en DTO ni `@Valid` en controller | **Media** | Validación |
| 12 | `IllegalArgumentException` para errores de dominio | **Media** | Arquitectura |
| 13 | Consola H2 habilitada sin scoping por perfil | **Media** | Seguridad |
| 14 | Acoplamiento y orden invertido en `EventoAporte` | **Media** | Diseño de dominio |
| 15 | Tests con estado compartido y orden dependiente | **Baja** | Testing |

**Veredicto: MR BLOQUEADO.** 5 hallazgos Críticos y 4 Altos deben resolverse antes de cualquier aprobación. Los hallazgos 1 (SQL Injection), 2 (`double`), 3 (race condition), 4 (`==` en tope) y 5 (idempotencia) en conjunto hacen que este módulo sea inseguro, financieramente impreciso e incumplidor de los estándares SFC bajo cualquier carga de producción realista.
