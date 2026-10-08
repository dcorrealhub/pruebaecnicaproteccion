# Revisión de código — `reto-a` · Registro de aportes a fondo voluntario

> Revisión hecha como si fuera un MR de la célula. **Criterio sobre cantidad:** primero lo que un revisor bloquearía antes de aprobar, después la deuda.
> Los hallazgos marcados con ✅ **Verificado** se reprodujeron con tests ejecutados contra una copia aislada del módulo (el código auditado no se modificó). En cada hallazgo, la sección «Test que lo demostraría» trae el test equivalente.

---

## 1. Contexto entendido

### Qué hace
El servicio Spring Boot 3.4.1 / Java 21 registra aportes voluntarios de afiliados y permite consultarlos por afiliado y periodo. Persiste en H2 en memoria con esquema generado por Hibernate (`ddl-auto=create-drop`) y datos semilla en `data.sql` (tres afiliados con saldo del mes `2025-06`).

### Flujo `POST /api/aportes`
```
JSON → AporteController.registrar(@RequestBody AporteRequest)    (sin @Valid)
     → AporteService.registrar(req)                              (sin @Transactional)
         1. monto <= 0 → IllegalArgumentException
         2. saldoRepo.findByAfiliadoId(afiliadoId)               (ignora el mes)
         3. nuevo = totalMes + monto; si nuevo == tope → error   (comparación de igualdad en double)
         4. saldoRepo.save(saldo)                                → COMMIT 1
         5. periodo = LocalDate.now() "yyyy-MM"; fecha = LocalDate.now()
         6. eventoRepo.save(new EventoAporte(aporte))            → COMMIT 2 (aporte aún sin id)
         7. aporteRepo.save(aporte)                              → COMMIT 3
     ← entidad JPA Aporte serializada tal cual (HTTP 200)
```

### Flujo `GET /api/aportes/consolidado`
```
afiliadoId, periodo (query params) → AporteController
   → SQL armado por concatenación → JdbcTemplate.query → RowMapper manual → List<Aporte> (entidad JPA)
```
El servicio y el repositorio no participan. `AporteJpaRepository.findByAfiliadoIdAndPeriodo` existe y nadie lo usa.

### Arquitectura real
Es una arquitectura en capas, nominal: `controller / service / domain / repository / dto`. El paquete `domain` son entidades JPA anémicas con `@Data`, así que el dominio depende de la persistencia. No hay puertos ni casos de uso, ni manejo global de errores, seguridad, migraciones o perfiles. El controller habla directamente con la base de datos.

### Invariantes de negocio que el código asume, y dónde se garantizan

| Invariante | ¿Se garantiza? | Dónde / por qué no |
|---|---|---|
| Monto > 0 | Parcial | `AporteService:36`. No rechaza `Infinity` ni escalas arbitrarias. No hay validación en el DTO. |
| Acumulado del mes ≤ tope mensual (10 M) | **No** | `AporteService:45` usa `==` (H-002). |
| El saldo del mes es la suma de los aportes del mes | **No** | Hay actualización perdida (H-003), escritura parcial (H-004), `double` (H-005) y una fila de saldo que no es mensual (H-006). |
| Un aporte se registra una sola vez | **No** | No hay idempotencia (H-008). |
| El periodo del aporte corresponde a la fecha de negocio | **No** | Se llama dos veces a `now()` con la zona del servidor (H-010). |
| Solo el afiliado o un canal autorizado registra o consulta | **No** | No hay seguridad (H-009). |
| Aportes > 5 M quedan marcados para revisión | Sí, por aporte individual | `AporteService:60`. Ver la pregunta abierta sobre fraccionamiento. |
| El afiliado existe y está activo | Parcial | Solo se comprueba que exista una fila de `saldo`. No existe el concepto de afiliado ni de su estado. |

---

## 2. Resumen ejecutivo

| Severidad | Cantidad |
|---|---|
| Crítica | 6 |
| Alta | 5 |
| Media | 6 |
| Baja | 1 |
| Informativa | 0 |

**Veredicto: ❌ No apto para merge.** Bastan cuatro revisiones de pocos minutos para rechazarlo:

1. **El controller ejecuta SQL concatenado con la entrada del usuario y mapea él mismo los resultados** (H-001, H-007). Es inyección SQL explotable y además rompe las capas.
2. **El tope mensual no se aplica**, porque se compara con `==` (H-002).
3. **El saldo se actualiza sin transacción y sin control de concurrencia** (H-003, H-004). Con 50 aportes concurrentes de $1.000 quedó un saldo de $9.000 en vez de $50.000.
4. **El dinero se maneja como `double`** (H-005). Un `monto: 1e400` deja el saldo del afiliado en `Infinity` de forma permanente.

La suite de tests pasa en verde con todos estos defectos (H-011).

---

## 3. Tabla de hallazgos

| ID | Severidad | Categoría | Ubicación | Título | Cómo lo corregiría |
|---|---|---|---|---|---|
| H-001 | Crítica | Seguridad | `controller/AporteController.java` | Inyección SQL en `GET /consolidado` ✅ | Consultar con el método parametrizado `findByAfiliadoIdAndPeriodo` (o `?` enlazados), nunca concatenando la entrada. |
| H-002 | Crítica | Lógica de negocio | `service/AporteService.java` | El tope mensual se valida con `==`: se puede superar ✅ | Rechazar cuando `nuevo.compareTo(tope) > 0`, con `BigDecimal`, dentro del dominio (`SaldoMensual.acreditar`). |
| H-003 | Crítica | Concurrencia | `service/AporteService.java`, `domain/Saldo.java` | Actualización perdida del saldo (read-modify-write sin bloqueo) ✅ | `@Version` con reintento, o `PESSIMISTIC_WRITE`, o `UPDATE ... SET total = total + :m WHERE total + :m <= :tope`. |
| H-004 | Crítica | Transacciones | `service/AporteService.java` | `registrar` sin `@Transactional`: tres commits independientes | `@Transactional` en el caso de uso público, para que saldo, aporte y evento se confirmen o reviertan juntos. |
| H-005 | Crítica | Lógica de negocio | `domain/*`, `dto/AporteRequest.java`, `service/AporteService.java`, `controller/AporteController.java` | Dinero modelado con `double` ✅ | Value Object `Monto` sobre `BigDecimal` (escala fija, > 0, finito) y columnas `NUMERIC(19,2)`. |
| H-006 | Crítica | Lógica de negocio / Persistencia | `service/AporteService.java`, `repository/SaldoJpaRepository.java` | El saldo «del mes» se busca ignorando el mes ✅ | Buscar por `(afiliadoId, mes)`, crear la fila del mes si no existe y poner `UNIQUE(afiliado_id, mes)`. |
| H-007 | Alta | Arquitectura | `controller/AporteController.java` | El controller accede a la BD y mapea filas, sin pasar por servicio ni repositorio | Quitar `JdbcTemplate` y `RowMapper` del controller y delegar en un servicio de consulta que use el repositorio y devuelva un DTO. |
| H-008 | Alta | Lógica de negocio / API | `controller/AporteController.java`, `service/AporteService.java` | Sin idempotencia: un reintento duplica el aporte y el saldo | Header `Idempotency-Key` obligatorio, persistido con restricción única; si se repite, devolver el aporte original. |
| H-009 | Alta | Seguridad | Todo el módulo (`pom.xml`, `AporteController.java`) | Endpoints sin autenticación ni autorización (IDOR por `afiliadoId`) | OAuth2 Resource Server (JWT) y autorización por afiliado (`@PreAuthorize`), o derivar el afiliado del token. |
| H-010 | Alta | Lógica de negocio | `service/AporteService.java`, `domain/EventoAporte.java` | Fecha y periodo dependen de la zona del servidor y de dos llamadas a `now()` | Inyectar un `Clock` en `America/Bogota` y derivar fecha, periodo y marca del evento de un único instante. |
| H-011 | Alta | Tests | `src/test/.../AporteServiceTest.java` | La suite no cubre ninguna invariante de dinero; los defectos críticos pasan en verde | Tests de frontera del tope, saldo = Σ aportes, concurrencia, rollback, `@WebMvcTest` y Testcontainers PostgreSQL. |
| H-012 | Media | Validación / API | `controller/AporteController.java`, `dto/AporteRequest.java` | Sin Bean Validation en la entrada (`afiliadoId` nulo, `canal` libre, escala del monto) | `@Valid` en el controller; `@NotBlank`, `@NotNull @Positive @Digits` y `enum Canal` en el DTO. |
| H-013 | Media | Manejo de errores / API | `controller/AporteController.java`, `service/AporteService.java` | Los errores de negocio responden 500; no hay contrato de error y el POST responde 200 | Excepciones de dominio y un `@RestControllerAdvice` con `ProblemDetail` (400/404/422); el POST debe devolver `201` + `Location`. |
| H-014 | Media | Arquitectura / API | `controller/AporteController.java` | Entidad JPA expuesta como contrato de respuesta | Un DTO `AporteResponse` y un mapper; paginar el consolidado. |
| H-015 | Media | Persistencia | `domain/*`, `application.properties` | Esquema generado por Hibernate, sin migraciones ni restricciones en BD | Flyway con `NOT NULL`, `UNIQUE`, `CHECK`, `NUMERIC` e índices; `ddl-auto=validate`. |
| H-016 | Media | Observabilidad / Auditoría | `domain/EventoAporte.java`, `service/AporteService.java` | El evento de auditoría no referencia al aporte ni al actor | Guardar primero el aporte; evento con `aporteId`, actor, canal y `correlationId`; log después del commit. |
| H-017 | Media | Configuración | `application.properties`, `data.sql` | Configuración de desarrollo como única configuración (sin perfiles) | Perfiles `local`/`prod`: H2, consola y `data.sql` solo en `local`; secretos por variables de entorno. |
| H-018 | Baja | Clean code / Persistencia | `domain/Aporte.java`, `domain/Saldo.java`, `domain/EventoAporte.java` | `@Data` en entidades JPA y modelo anémico | `@Getter` más métodos de dominio en lugar de setters; `equals/hashCode` por id o ninguno. |

Todas las rutas son relativas a `reto-a/src/main/java/co/proteccion/cis/retoa/`, salvo que se indique otra cosa.

---

## 4. Detalle de hallazgos

### H-001 — Inyección SQL en `GET /api/aportes/consolidado` ✅ Verificado
- **Ubicación:** `reto-a/src/main/java/co/proteccion/cis/retoa/controller/AporteController.java`, método `consolidado()`, líneas 38–44.
- **Severidad:** Crítica
- **Categoría:** Seguridad
- **Evidencia:**
  ```java
  String sql = "SELECT * FROM aporte WHERE afiliado_id = '"
          + afiliadoId + "' AND periodo = '" + periodo + "'";
  return jdbc.query(sql, aporteRowMapper);
  ```
- **Motivo:** Ambos parámetros vienen sin sanear de la query string. Con `afiliadoId = x' OR '1'='1' --` la consulta devolvió **los aportes de todos los afiliados**, cuando debía devolver los de uno solo. Lo verifiqué: la respuesta incluyó AF-001 y AF-002. Con `UNION SELECT` se puede leer cualquier tabla (`saldo`, `evento_aporte`). Si el motor acepta varias sentencias, también se pueden alterar datos. El impacto es una fuga masiva de información financiera de afiliados y un posible incumplimiento de protección de datos. No hay ningún escenario en que esto sea una decisión de diseño.
- **Solución:** Eliminar el SQL del controller (ver H-007) y usar el método derivado que ya existe, que está parametrizado:
  ```java
  // AporteController
  @GetMapping("/consolidado")
  public List<AporteResponse> consolidado(@RequestParam @Pattern(regexp = "AF-\\d+") String afiliadoId,
                                          @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth periodo) {
      return consultarAportes.porAfiliadoYPeriodo(afiliadoId, periodo);
  }
  // Servicio de consulta → aporteRepo.findByAfiliadoIdAndPeriodo(afiliadoId, periodo.toString())
  ```
  Si de verdad se necesita JDBC (por ejemplo, un read model CQRS), debe ir en un adaptador de persistencia y con parámetros enlazados: `jdbc.query("... WHERE afiliado_id = ? AND periodo = ?", mapper, afiliadoId, periodo)`.
- **Test que lo demostraría:** `@WebMvcTest` o `@SpringBootTest` + MockMvc: registrar aportes de AF-001 y AF-002, llamar `GET /consolidado?afiliadoId=x' OR '1'='1' --&periodo=x` y verificar que la respuesta sea `400` o una lista vacía. Hoy devuelve los dos aportes.

---

### H-002 — El tope mensual se valida con `==`: se puede superar ✅ Verificado
- **Ubicación:** `reto-a/src/main/java/co/proteccion/cis/retoa/service/AporteService.java`, método `registrar()`, líneas 43–47.
- **Severidad:** Crítica
- **Categoría:** Lógica de negocio
- **Evidencia:**
  ```java
  double nuevo = s.getTotalMes() + monto;
  if (nuevo == topeMensual) {
      throw new IllegalArgumentException("El monto supera el tope mensual permitido");
  }
  ```
- **Motivo:** La condición solo rechaza el caso en que el acumulado es **exactamente** igual al tope, que además es probablemente el único valor que sí debería aceptarse. Verificado con AF-003, que tiene un saldo de 4,5 M:
  - +6 M → acumulado de 10,5 M **aceptado** (supera el tope).
  - +5,5 M → acumulado de 10 M **rechazado** con el mensaje «supera el tope».

  Cualquier monto mayor al tope pasa. Como además se compara igualdad entre `double`, incluso el caso límite depende de errores de representación. El tope queda anulado como control, con impacto regulatorio y financiero si el límite tiene origen normativo o de producto.
- **Solución:** Comparar con `compareTo` sobre `BigDecimal` (ver H-005) y llevar la regla al dominio:
  ```java
  // dentro de SaldoMensual (dominio)
  public void acreditar(Monto monto, Monto tope) {
      Monto nuevo = this.total.mas(monto);
      if (nuevo.esMayorQue(tope)) {              // compareTo(...) > 0
          throw new TopeMensualExcedidoException(afiliadoId, mes, tope);
      }
      this.total = nuevo;
  }
  ```
  Hay que confirmar si el tope es inclusivo (ver Preguntas abiertas).
- **Test que lo demostraría:** Tests parametrizados sobre AF-003 (4,5 M): con 5.499.999 se acepta, con 5.500.000 se acepta (si el tope es inclusivo) y con 5.500.001 se rechaza con `TopeMensualExcedidoException`, verificando además que el saldo no cambió.

---

### H-003 — Actualización perdida del saldo (read-modify-write sin bloqueo) ✅ Verificado
- **Ubicación:** `reto-a/src/main/java/co/proteccion/cis/retoa/service/AporteService.java`, `registrar()`, líneas 40–50. También `reto-a/src/main/java/co/proteccion/cis/retoa/domain/Saldo.java`, que no tiene `@Version`.
- **Severidad:** Crítica
- **Categoría:** Concurrencia
- **Evidencia:**
  ```java
  Saldo s = saldoRepo.findByAfiliadoId(req.getAfiliadoId())...   // lee
  double nuevo = s.getTotalMes() + monto;                          // calcula en memoria
  s.setTotalMes(nuevo);
  saldoRepo.save(s);                                               // escribe: last-write-wins
  ```
- **Motivo:** Dos aportes simultáneos del mismo afiliado (app y web, o un doble envío) leen el mismo `totalMes`, y el último en escribir pisa al otro. Verificado con 50 aportes concurrentes de $1.000 a AF-002: se registraron 50 aportes por $50.000, pero el **saldo quedó en $9.000**. Hay dos consecuencias:
  1. El saldo deja de cuadrar con la suma de los aportes.
  2. El tope mensual se evade, porque cada hilo valida contra un saldo desactualizado.

  Agregar `@Transactional` por sí solo **no** lo corrige, porque con READ COMMITTED (el valor por defecto en H2 y PostgreSQL) la actualización perdida persiste.
- **Solución (elegir una):**
  - **Bloqueo optimista**: `@Version private Long version;` en `Saldo`, con reintento acotado ante `ObjectOptimisticLockingFailureException`. Es lo adecuado cuando hay poca contención por afiliado.
  - **Bloqueo pesimista** sobre la fila del mes:
    ```java
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Saldo s where s.afiliadoId = :afiliadoId and s.mes = :mes")
    Optional<Saldo> findParaActualizar(String afiliadoId, String mes);
    ```
  - **Actualización atómica condicionada** en BD, que resuelve también el tope: `UPDATE saldo SET total_mes = total_mes + :monto WHERE afiliado_id = :a AND mes = :m AND total_mes + :monto <= :tope`. Si afecta 0 filas, se rechaza el aporte.
- **Test que lo demostraría:** Test de integración (idealmente con Testcontainers PostgreSQL): lanzar N hilos con un `CountDownLatch` que registren un aporte de $1.000 cada uno y verificar `saldo.totalMes == suma(aportes del mes)`. Un segundo test debe comprobar que, cuando N aportes concurrentes superan el tope, solo se aceptan los que caben.

---

### H-004 — `registrar` sin `@Transactional`: tres commits independientes
- **Ubicación:** `reto-a/src/main/java/co/proteccion/cis/retoa/service/AporteService.java`, `registrar()`, líneas 33–67.
- **Severidad:** Crítica
- **Categoría:** Transacciones
- **Evidencia:**
  ```java
  public Aporte registrar(AporteRequest req) {   // sin @Transactional
      ...
      saldoRepo.save(s);                          // commit 1
      ...
      eventoRepo.save(new EventoAporte(aporte));  // commit 2
      ...
      return aporteRepo.save(aporte);             // commit 3
  }
  ```
- **Motivo:** Cada `save` de Spring Data abre y confirma su propia transacción. Si falla el insert del aporte (BD caída, timeout, una futura restricción, el pool agotado), el saldo **ya quedó incrementado** y el evento «APORTE_REGISTRADO» **ya quedó escrito**, pero el aporte no existe. El resultado es un saldo inflado sin respaldo y una auditoría que afirma algo falso. El cliente recibe un error, reintenta (ver H-008) y el saldo se infla otra vez. Es corrupción de datos financieros sin ningún mecanismo de reconciliación.
- **Solución:** Hacer que el caso de uso sea una unidad atómica. La anotación va en el método público invocado desde fuera del bean, para que el proxy la aplique:
  ```java
  @Transactional
  public AporteRegistrado registrar(RegistrarAporteCommand cmd) { ... }
  ```
  Las excepciones de negocio deben ser `RuntimeException` (o declararse con `rollbackFor`). Si el evento se va a publicar a un broker, conviene usar un outbox en la misma transacción o `@TransactionalEventListener(phase = AFTER_COMMIT)`.
- **Test que lo demostraría:** `@SpringBootTest` con `@MockitoSpyBean AporteJpaRepository` configurado para lanzar `DataAccessResourceFailureException` en `save`. Después de invocar `registrar`, se verifica que `saldo.totalMes` no cambió y que `evento_aporte` sigue vacía. Hoy fallaría en las dos verificaciones.

---

### H-005 — Dinero modelado con `double` ✅ Verificado
- **Ubicación:**
  - `reto-a/src/main/java/co/proteccion/cis/retoa/domain/Aporte.java:24`: `private double monto;`
  - `reto-a/src/main/java/co/proteccion/cis/retoa/domain/Saldo.java:20`: `private double totalMes;`
  - `reto-a/src/main/java/co/proteccion/cis/retoa/domain/EventoAporte.java:21`: `private double monto;`
  - `reto-a/src/main/java/co/proteccion/cis/retoa/dto/AporteRequest.java:15`: `private double monto;`
  - `reto-a/src/main/java/co/proteccion/cis/retoa/service/AporteService.java:28,31,34,43`: `topeMensual`, `umbralRevision`, la aritmética.
  - `reto-a/src/main/java/co/proteccion/cis/retoa/controller/AporteController.java:25`: `rs.getDouble("monto")`.
- **Severidad:** Crítica
- **Categoría:** Lógica de negocio
- **Evidencia:**
  ```java
  private double monto;            // entidad, DTO y evento
  double nuevo = s.getTotalMes() + monto;
  ```
- **Motivo:** Hay dos escenarios verificados:
  1. **Errores de representación:** diez aportes de 0,10 dejaron un saldo de `0.9999999999999999`. El acumulado deriva respecto de la suma real y las comparaciones contra el tope o el umbral se vuelven inciertas.
  2. **Valores no finitos:** un `POST` con `"monto": 1e400` fue **aceptado con HTTP 200**. Jackson lo convierte en `Infinity`, que pasa `monto <= 0`, se persiste el aporte con `"monto":"Infinity"` y **el saldo del afiliado quedó en `Infinity`** de forma permanente. Ningún aporte posterior puede volver a validarse contra el tope.

  Además, el esquema que genera Hibernate usa `DOUBLE PRECISION` en BD, así que el error se arrastra a reportes y conciliaciones.
- **Solución:** Usar `BigDecimal` de punta a punta, con escala y redondeo explícitos, encapsulado en un Value Object:
  ```java
  public record Monto(BigDecimal valor) {
      private static final int ESCALA = 2;                 // confirmar con negocio (ver preguntas)
      public Monto {
          Objects.requireNonNull(valor);
          if (valor.signum() <= 0) throw new MontoInvalidoException(valor);
          if (valor.scale() > ESCALA) throw new MontoInvalidoException(valor);
          valor = valor.setScale(ESCALA, RoundingMode.UNNECESSARY);
      }
      public Monto mas(Monto o) { return new Monto(valor.add(o.valor)); }
      public boolean esMayorQue(Monto o) { return valor.compareTo(o.valor) > 0; }
  }
  ```
  - En la entidad: `@Column(precision = 19, scale = 2, nullable = false) private BigDecimal monto;`
  - En el DTO: `@NotNull @Positive @Digits(integer = 15, fraction = 2) BigDecimal monto`.
  - En la configuración: `@Value("${aporte.tope-mensual}") BigDecimal topeMensual` o un `@ConfigurationProperties` tipado.
- **Test que lo demostraría:**
  - `registrar` diez veces 0,10 y verificar que el saldo sea `compareTo(new BigDecimal("1.00")) == 0`.
  - `POST` con `monto: 1e400`, `monto: 0.001` y `monto: 1e20`: los tres deben responder `400`.

---

### H-006 — El saldo «del mes» se busca ignorando el mes ✅ Verificado
- **Ubicación:**
  - `reto-a/src/main/java/co/proteccion/cis/retoa/repository/SaldoJpaRepository.java:10`: `findByAfiliadoId`.
  - `reto-a/src/main/java/co/proteccion/cis/retoa/service/AporteService.java:40–52`.
  - `reto-a/src/main/java/co/proteccion/cis/retoa/domain/Saldo.java:19–23`.
- **Severidad:** Crítica
- **Categoría:** Lógica de negocio / Persistencia
- **Evidencia:**
  ```java
  // Saldo: "Acumulado del mes" + campo mes (YYYY-MM)
  Optional<Saldo> findByAfiliadoId(String afiliadoId);      // no filtra por mes
  ...
  Saldo s = saldoRepo.findByAfiliadoId(req.getAfiliadoId())  // usa la fila que exista
  String periodo = LocalDate.now()...                         // el periodo del aporte se calcula aparte
  ```
- **Motivo:** `Saldo` representa el acumulado **de un mes**, pero la búsqueda no usa el mes y nada crea la fila del mes en curso. Esto produce tres efectos:
  1. Hoy (2026-10) los aportes con periodo `2026-10` se suman a la fila de `2025-06`. Verificado: el aporte se registró con `periodo=2026-10` contra el saldo de junio de 2025. El saldo de junio queda alterado y el tope «mensual» se convierte en un tope histórico, de modo que se rechazarán aportes legítimos cuando se acumulen 10 M en total.
  2. Si alguien crea la fila del mes siguiente, que es lo natural en un modelo mensual, `findByAfiliadoId` lanza `IncorrectResultSizeDataAccessException`. Verificado: con dos filas para AF-001, **todo aporte del afiliado falla con 500**.
  3. «Afiliado no encontrado» se confunde con «no tiene saldo abierto».
- **Solución:** Buscar por `(afiliadoId, mes)`, con el mes calculado una sola vez a partir de la fecha de negocio (H-010). Crear la fila del mes si no existe, de forma segura ante concurrencia gracias a la restricción única de H-015:
  ```java
  Optional<Saldo> findByAfiliadoIdAndMes(String afiliadoId, String mes);

  YearMonth mes = YearMonth.from(fechaNegocio);
  Saldo saldo = saldoRepo.findParaActualizar(afiliadoId, mes.toString())
        .orElseGet(() -> saldoRepo.save(Saldo.abrir(afiliadoId, mes)));   // + UNIQUE(afiliado_id, mes)
  ```
  La existencia y el estado del afiliado deben validarse contra su propia fuente (ver Preguntas abiertas), no inferirse de la tabla `saldo`.
- **Test que lo demostraría:** Con un `Clock` fijo en 2025-07-01 y una fila de saldo de 2025-06 con 9 M, registrar 5 M debe aceptarse y crear el saldo de 2025-07 en 5 M, sin modificar junio. Con filas de dos meses para el mismo afiliado, el registro no debe fallar.

---

### H-007 — El controller accede a la BD y mapea filas, sin pasar por servicio ni repositorio
- **Ubicación:** `reto-a/src/main/java/co/proteccion/cis/retoa/controller/AporteController.java`: campos `jdbc` y `aporteRowMapper` (líneas 19–31) y método `consolidado()` (38–44).
- **Severidad:** Alta
- **Categoría:** Arquitectura
- **Evidencia:**
  ```java
  private final JdbcTemplate jdbc;
  private final RowMapper<Aporte> aporteRowMapper = (rs, rowNum) -> {
      Aporte a = new Aporte();
      a.setMonto(rs.getDouble("monto"));
      a.setFecha(rs.getDate("fecha").toLocalDate());
      ...
  };
  ```
- **Motivo:** Esto bastaría para rechazar el MR en una primera lectura, independientemente de la inyección (H-001), que se reporta aparte:
  - La capa web conoce nombres de tablas y columnas. Cualquier cambio de esquema o de motor obliga a tocar el controller, y el mapeo queda duplicado con el de JPA. El `RowMapper` usa `getDouble`, divergente de lo que debe ser el tipo monetario.
  - La consulta se salta el servicio, así que cualquier regla futura de autorización (H-009), de enmascaramiento o de auditoría de consultas tendría que repetirse en el controller.
  - Ya existe `AporteJpaRepository.findByAfiliadoIdAndPeriodo`, que hace exactamente esto de forma parametrizada. El código nuevo duplica una capacidad y abre la vulnerabilidad.
  - Es frágil: `rs.getDate("fecha").toLocalDate()` lanza NPE si `fecha` es nula, y la BD lo permite (H-015).
  - El controller acumula dos responsabilidades (HTTP y acceso a datos) y depende de `JdbcTemplate`, que es infraestructura.
- **Solución:** El controller solo traduce HTTP ↔ caso de uso. La consulta va a un servicio o *query handler* (CQRS de lectura) que usa un puerto de repositorio. El mapeo a DTO de respuesta se hace en un mapper dedicado. Hay que retirar `JdbcTemplate` del controller y `spring-boot-starter-jdbc` del `pom.xml` si no tiene otro uso.
  ```java
  @RestController @RequestMapping("/api/v1/aportes") @RequiredArgsConstructor
  class AporteController {
      private final RegistrarAporteUseCase registrar;
      private final ConsultarAportesQuery consultar;
      ...
  }
  ```
- **Test que lo demostraría:** Un test de arquitectura con ArchUnit: `noClasses().that().resideInAPackage("..controller..").should().dependOnClassesThat().resideInAnyPackage("org.springframework.jdbc..", "..repository..", "jakarta.persistence..")`.

---

### H-008 — Sin idempotencia: un reintento duplica el aporte y el saldo
- **Ubicación:** `reto-a/src/main/java/co/proteccion/cis/retoa/controller/AporteController.java`, `registrar()` (33–36), y `reto-a/src/main/java/co/proteccion/cis/retoa/service/AporteService.java`, `registrar()`.
- **Severidad:** Alta
- **Categoría:** Lógica de negocio / API
- **Evidencia:**
  ```java
  @PostMapping
  public Aporte registrar(@RequestBody AporteRequest req) { return service.registrar(req); }
  // AporteRequest: afiliadoId, monto, canal. No hay identificador de la operación.
  ```
- **Motivo:** Los canales declarados (`APP_MOVIL`, `WEB`) reintentan por diseño ante timeouts, y los usuarios hacen doble clic. Cada reintento crea un aporte nuevo, suma de nuevo al saldo y consume cupo del tope. No hay clave de idempotencia ni restricción única que lo impida. El resultado es un aporte duplicado frente a un único recaudo real: descuadre y reclamación del afiliado. Se clasifica como Alta y no como Crítica porque depende del comportamiento del cliente, pero en móvil ese escenario es habitual.
- **Solución:** Exigir un header `Idempotency-Key` (o un `idOperacion` generado por el canal) y persistirlo con restricción única en `aporte`. Ante una clave repetida se devuelve el aporte original (`200`) en lugar de registrar otro. Si el payload difiere, se responde `409`.
  ```java
  @PostMapping
  ResponseEntity<AporteResponse> registrar(@RequestHeader("Idempotency-Key") @NotBlank String key,
                                           @Valid @RequestBody AporteRequest req) { ... }
  // aporte.idempotency_key UNIQUE NOT NULL
  ```
- **Test que lo demostraría:** Dos `POST` con la misma `Idempotency-Key` deben dejar un solo aporte y el saldo incrementado una sola vez. El mismo caso, lanzado de forma concurrente, debe producir un `201` y un `200` (o `409`), nunca dos aportes.

---

### H-009 — Endpoints sin autenticación ni autorización (IDOR por `afiliadoId`)
- **Ubicación:** `reto-a/pom.xml`, que no incluye `spring-boot-starter-security` ni un resource server. También `reto-a/src/main/java/co/proteccion/cis/retoa/controller/AporteController.java`, en ambos endpoints.
- **Severidad:** Alta (sube a Crítica si no hay un gateway que autentique; ver Preguntas abiertas)
- **Categoría:** Seguridad
- **Evidencia:**
  ```java
  public List<Aporte> consolidado(@RequestParam String afiliadoId, @RequestParam String periodo)
  // El afiliado consultado o abonado lo decide el cliente, sin relación con una identidad autenticada.
  ```
- **Motivo:** Cualquier cliente con acceso de red puede consultar los aportes de cualquier afiliado con solo cambiar `afiliadoId`, y puede registrar aportes a nombre de terceros. Aunque un gateway autentique, el servicio no comprueba que el sujeto del token pueda operar sobre ese `afiliadoId`, que es un IDOR clásico. Tampoco queda registro de **quién** registró el aporte (ver H-016).
- **Solución:** Configurar el servicio como OAuth2 Resource Server (JWT). Derivar el afiliado del token para canales de autoservicio, o validar con autorización a nivel de método que el usuario o canal autenticado puede operar sobre ese afiliado:
  ```java
  @PreAuthorize("@autorizacionAfiliado.puedeOperar(authentication, #afiliadoId)")
  ```
- **Test que lo demostraría:** `@WebMvcTest` + `spring-security-test`: sin token se espera `401`, y con un token de AF-001 consultando a AF-002 se espera `403`.

---

### H-010 — Fecha y periodo dependen de la zona del servidor y de dos llamadas a `now()`
- **Ubicación:** `reto-a/src/main/java/co/proteccion/cis/retoa/service/AporteService.java:52` y `:57`. También `reto-a/src/main/java/co/proteccion/cis/retoa/domain/EventoAporte.java:31`.
- **Severidad:** Alta
- **Categoría:** Lógica de negocio
- **Evidencia:**
  ```java
  String periodo = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
  ...
  aporte.setFecha(LocalDate.now());
  // EventoAporte: this.fechaEvento = LocalDateTime.now();
  ```
- **Motivo:**
  1. `LocalDate.now()` usa la zona por defecto de la JVM. En contenedores suele ser UTC, cinco horas por delante de Colombia. Un aporte hecho el 30 de junio a las 20:00 hora de Bogotá queda con `fecha=2025-07-01` y `periodo=2025-07`. Cuenta para el tope y el consolidado del mes equivocado. Ocurre todos los fines de mes y se agrava cuando se combina con cortes tributarios de diciembre.
  2. `periodo` y `fecha` se calculan con dos llamadas distintas. Si el registro cruza la medianoche del último día del mes, el mismo aporte puede quedar con `periodo=2025-06` y `fecha=2025-07-01`, inconsistente consigo mismo.
  3. El código no se puede probar de forma determinista porque no hay un `Clock` inyectable. Por eso no existen tests de corte de mes.
- **Solución:** Inyectar un `Clock` con la zona de negocio. Calcular un único instante por operación y derivar de él la fecha, el periodo y la marca del evento. Guardar el instante como `Instant`/`OffsetDateTime` para auditoría.
  ```java
  @Bean Clock clock() { return Clock.system(ZoneId.of("America/Bogota")); }
  ...
  ZonedDateTime ahora = ZonedDateTime.now(clock);
  LocalDate fecha = ahora.toLocalDate();
  YearMonth periodo = YearMonth.from(fecha);
  ```
- **Test que lo demostraría:** Con `Clock.fixed(Instant.parse("2025-07-01T01:00:00Z"), ZoneId.of("America/Bogota"))` (30 de junio a las 20:00 en Bogotá), el aporte debe quedar con `periodo = 2025-06` y `fecha = 2025-06-30`.

---

### H-011 — La suite no cubre ninguna invariante de dinero; los defectos críticos pasan en verde
- **Ubicación:** `reto-a/src/test/java/co/proteccion/cis/retoa/AporteServiceTest.java`, todo el archivo.
- **Severidad:** Alta
- **Categoría:** Tests
- **Evidencia:**
  ```java
  @SpringBootTest
  class AporteServiceTest {          // 4 tests. Ninguno verifica el saldo, el tope, el controller ni la concurrencia.
      void registrar_montoValido_retornaAporte() { ... assertEquals(500_000.0, result.getMonto()); }
  ```
- **Motivo:** La suite pasa con los seis defectos críticos presentes, así que no protege nada de lo que importa:
  - **Tope:** ningún test lo cubre. Un test de frontera habría detectado el `==` de H-002 de inmediato.
  - **Saldo:** ningún test lee el saldo después de registrar (H-003, H-004, H-005).
  - **Controller:** no hay `@WebMvcTest`, así que no hay contrato HTTP ni prueba de la inyección (H-001) ni de los códigos de error (H-013).
  - **Límites:** faltan monto `0`, monto muy grande o no finito, decimales y `afiliadoId` nulo.
  - **Concurrencia e idempotencia:** no hay ningún test.
  - **Fechas:** no hay test de corte de mes porque no hay `Clock`.
  - **Fragilidad:** los tests comparten el estado de H2 y mutan el saldo de AF-001 sin limpieza. El resultado depende del orden de ejecución, y en cuanto exista un test de tope sobre AF-001 los tests interferirán entre sí.
  - **Motor:** se prueba contra H2, mientras el stack de referencia es PostgreSQL. El comportamiento de bloqueos, aislamiento y tipos numéricos difiere.
  - **Aserciones débiles:** `assertThrows(IllegalArgumentException.class, ...)` no distingue «monto inválido» de «afiliado inexistente», porque ambos lanzan la misma excepción genérica.
- **Solución:**
  - Tests unitarios del dominio (`Monto`, `SaldoMensual.acreditar`), sin Spring y con casos de frontera parametrizados.
  - `@WebMvcTest` del controller para el contrato y los errores.
  - `@DataJpaTest` + Testcontainers PostgreSQL para el repositorio, las restricciones y la concurrencia.
  - Aislamiento entre tests: `@Transactional` en tests sin concurrencia, o `@Sql` de limpieza.
  - Excepciones de negocio específicas, para poder verificar la causa del error.
- **Test que lo demostraría:** Los listados en «Test que lo demostraría» de H-001 a H-010. Como criterio de aceptación, cada invariante de la tabla de la sección 1 debe tener al menos un test que falle si se rompe.

---

### H-012 — Sin Bean Validation en la entrada
- **Ubicación:** `reto-a/src/main/java/co/proteccion/cis/retoa/dto/AporteRequest.java`, todo el archivo, y `reto-a/src/main/java/co/proteccion/cis/retoa/controller/AporteController.java:34` (falta `@Valid`).
- **Severidad:** Media
- **Categoría:** Validación / API
- **Evidencia:**
  ```java
  public Aporte registrar(@RequestBody AporteRequest req)   // sin @Valid
  private String afiliadoId;  private double monto;  private String canal;   // sin restricciones
  ```
- **Motivo:**
  - **Canal:** `canal` acepta cualquier texto o `null`, y se persiste tal cual. Verificado: se puede enviar `"cualquier cosa"`. Los reportes por canal y la conciliación con los canales reales quedan sucios.
  - **Afiliado nulo:** un `afiliadoId` ausente llega hasta la BD y vuelve como «Afiliado no encontrado: null» con status 500.
  - **Monto:** como es primitivo, un `monto` ausente se convierte silenciosamente en `0`.
  - **Formato del periodo:** en el GET, `periodo` no se valida contra `yyyy-MM`.

  El impacto es acotado porque el servicio rechaza algunos de estos casos, pero el error llega tarde y como 500.
- **Solución:**
  ```java
  public record AporteRequest(
      @NotBlank @Pattern(regexp = "AF-\\d{3,}") String afiliadoId,
      @NotNull @Positive @Digits(integer = 15, fraction = 2) BigDecimal monto,
      @NotNull Canal canal) {}            // enum Canal { APP_MOVIL, WEB, ... }
  ```
  Agregar `@Valid` en el controller y `@Validated` con `YearMonth` para los parámetros del GET. Las invariantes también deben vivir en el dominio (`Monto`, H-005), porque la validación del DTO no protege otros puntos de entrada.
- **Test que lo demostraría:** `@WebMvcTest` parametrizado con `afiliadoId` nulo, `canal` inválido, `monto` ausente y `periodo=2025-13`: todos deben responder `400` con detalle del campo, y el servicio no debe invocarse.

---

### H-013 — Los errores de negocio responden 500; no hay contrato de error y el POST responde 200
- **Ubicación:** `reto-a/src/main/java/co/proteccion/cis/retoa/service/AporteService.java:37,41,46` (`IllegalArgumentException`) y el módulo completo, que no tiene `@RestControllerAdvice`.
- **Severidad:** Media
- **Categoría:** Manejo de errores / API
- **Evidencia:**
  ```java
  throw new IllegalArgumentException("El monto debe ser positivo");
  throw new IllegalArgumentException("Afiliado no encontrado: " + ...);
  throw new IllegalArgumentException("El monto supera el tope mensual permitido");
  ```
- **Motivo:** Verificado: un monto negativo o un afiliado nulo terminan en una excepción no manejada, es decir, en un HTTP 500. Hay tres consecuencias:
  - Los canales no pueden distinguir un error del cliente (400/422), un afiliado inexistente (404/422) o un tope excedido (422) de una falla del servidor.
  - Gateways y clientes suelen reintentar automáticamente los 5xx, lo que genera tráfico inútil y, combinado con H-008, más riesgo.
  - Las alertas de 5xx se llenan de ruido de negocio.

  Además, `IllegalArgumentException` es demasiado genérica: un `IllegalArgumentException` lanzado por cualquier librería se trataría como regla de negocio. El `POST` responde `200` en lugar de `201` y no incluye `Location`.
- **Solución:** Definir excepciones de dominio (`MontoInvalidoException`, `AfiliadoNoEncontradoException`, `TopeMensualExcedidoException`) y un `@RestControllerAdvice` que las traduzca a `ProblemDetail` (RFC 9457) con el código adecuado, sin exponer trazas. El POST debe responder `ResponseEntity.created(uri).body(...)`. También conviene versionar la ruta (`/api/v1/aportes`).
- **Test que lo demostraría:** `@WebMvcTest`: tope excedido → `422` con `type` o `code` = `TOPE_MENSUAL_EXCEDIDO`; afiliado inexistente → `404`/`422`; registro exitoso → `201` con header `Location`.

---

### H-014 — Entidad JPA expuesta como contrato de respuesta
- **Ubicación:** `reto-a/src/main/java/co/proteccion/cis/retoa/controller/AporteController.java:34` (`Aporte registrar(...)`) y `:39` (`List<Aporte> consolidado(...)`).
- **Severidad:** Media
- **Categoría:** Arquitectura / API
- **Evidencia:**
  ```java
  public Aporte registrar(@RequestBody AporteRequest req)
  public List<Aporte> consolidado(...)
  ```
- **Motivo:** El contrato HTTP queda acoplado al esquema de persistencia: renombrar una columna o agregar un campo interno cambia la API sin que nadie lo decida. Hoy esto ya expone `marcadaRevision` (un indicador de control interno, ver Preguntas abiertas) y el `id` autoincremental, que es enumerable y facilita el IDOR (H-009). Además, `consolidado` devuelve una lista sin paginar: un afiliado con muchos aportes en el periodo devuelve todo de una vez.
- **Solución:** Usar un DTO de respuesta explícito (`record AporteResponse(String id, BigDecimal monto, LocalDate fecha, Canal canal, YearMonth periodo)`) y un mapper. Si el «consolidado» debe ser un total, se puede devolver `ConsolidadoResponse(total, cantidad, aportes)` paginado.
- **Test que lo demostraría:** Un test de contrato con `@WebMvcTest` y `jsonPath`: verificar que la respuesta no contiene `marcadaRevision` y que su forma coincide con la especificación OpenAPI acordada.

---

### H-015 — Esquema generado por Hibernate, sin migraciones ni restricciones en BD
- **Ubicación:** `reto-a/src/main/resources/application.properties:9` (`ddl-auto=create-drop`) y las entidades `reto-a/src/main/java/co/proteccion/cis/retoa/domain/Aporte.java`, `Saldo.java` y `EventoAporte.java`, que no tienen `@Column(nullable=false)`, `@UniqueConstraint` ni `precision/scale`.
- **Severidad:** Media
- **Categoría:** Persistencia
- **Evidencia:**
  ```properties
  spring.jpa.hibernate.ddl-auto=create-drop
  ```
  ```java
  private String afiliadoId;   // nullable, sin índice
  private String mes;          // sin UNIQUE(afiliado_id, mes)
  ```
- **Motivo:** Faltan las restricciones que respaldarían en BD las correcciones de H-003, H-006 y H-008:
  - No hay `UNIQUE(afiliado_id, mes)` en `saldo`, justo lo que permitió las filas duplicadas que rompen el registro en H-006.
  - No hay `UNIQUE(idempotency_key)`.
  - Las columnas clave no tienen `NOT NULL` (`afiliado_id`, `monto`, `fecha`, `periodo`).
  - No hay `CHECK (monto > 0)`.
  - Los montos usan `DOUBLE` en lugar de `NUMERIC(19,2)`.
  - No hay índice en `aporte(afiliado_id, periodo)`, que es el filtro de la consulta.

  Sin migraciones versionadas (Flyway o Liquibase), el esquema no puede revisarse en un MR ni evolucionar sin pérdida, y `create-drop` contra una BD persistente borra todo al apagar la aplicación.
- **Solución:** Agregar Flyway (`V1__esquema_inicial.sql`) con tipos y restricciones explícitas, poner `ddl-auto=validate` y anotar las entidades para que coincidan con la migración.
  ```sql
  CREATE TABLE saldo (id BIGSERIAL PRIMARY KEY, afiliado_id VARCHAR(20) NOT NULL, mes CHAR(7) NOT NULL,
                      total_mes NUMERIC(19,2) NOT NULL DEFAULT 0, version BIGINT NOT NULL DEFAULT 0,
                      CONSTRAINT uk_saldo_afiliado_mes UNIQUE (afiliado_id, mes));
  CREATE INDEX ix_aporte_afiliado_periodo ON aporte (afiliado_id, periodo);
  ```
- **Test que lo demostraría:** Un `@DataJpaTest` con Testcontainers: insertar dos saldos con el mismo `(afiliado_id, mes)` debe lanzar `DataIntegrityViolationException`, y el arranque con `ddl-auto=validate` debe pasar.

---

### H-016 — El evento de auditoría no referencia al aporte ni al actor
- **Ubicación:** `reto-a/src/main/java/co/proteccion/cis/retoa/domain/EventoAporte.java:27–32` y `reto-a/src/main/java/co/proteccion/cis/retoa/service/AporteService.java:62–66`.
- **Severidad:** Media
- **Categoría:** Observabilidad / Auditoría
- **Evidencia:**
  ```java
  eventoRepo.save(new EventoAporte(aporte));   // el aporte todavía no tiene id
  log.info("Aporte registrado: monto={} afiliado={}", monto, req.getAfiliadoId());   // se escribe antes del save del aporte
  return aporteRepo.save(aporte);
  ```
- **Motivo:** El evento se construye antes de persistir el aporte, así que no guarda `aporteId`. Si un afiliado tiene dos aportes iguales el mismo día, no se puede saber a cuál corresponde cada evento. Tampoco registra quién originó la operación (usuario o canal autenticado), ni el canal, ni un id de correlación. El log de «registrado» se escribe antes de que el aporte exista, de modo que si el insert falla el log afirma algo falso, sin id de aporte ni traza. Ante una reclamación o una auditoría no se puede reconstruir quién registró qué. `tipo` es un string mágico.
- **Solución:** Persistir primero el aporte, crear el evento con `aporteId`, el actor, el canal, `correlationId` y un `Instant`, dentro de la misma transacción (H-004). Usar un `enum TipoEvento`. Registrar el log después del commit (o con `@TransactionalEventListener`) con `aporteId` y `correlationId` vía MDC.
- **Test que lo demostraría:** Después de registrar un aporte, `evento_aporte` debe contener exactamente un registro con `aporteId` igual al id devuelto y con el actor autenticado.

---

### H-017 — Configuración de desarrollo como única configuración (sin perfiles)
- **Ubicación:** `reto-a/src/main/resources/application.properties` (líneas 4–15) y `reto-a/src/main/resources/data.sql`.
- **Severidad:** Media
- **Categoría:** Configuración
- **Evidencia:**
  ```properties
  spring.datasource.url=jdbc:h2:mem:retodb;...
  spring.datasource.username=sa
  spring.datasource.password=
  spring.h2.console.enabled=true
  ```
- **Motivo:** No existen perfiles, así que lo que se despliega es exactamente esta configuración:
  - BD en memoria: cada reinicio pierde todos los aportes.
  - Consola H2 habilitada. Por defecto solo admite conexiones locales (`web-allow-others=false`), lo que limita su alcance, pero sigue siendo una consola SQL con usuario `sa` sin contraseña.
  - `data.sql` con afiliados sintéticos que se insertan al arrancar.
  - Credenciales embebidas en el archivo.

  Es aceptable para la prueba, pero en un MR hacia un entorno real hace falta separar la configuración.
- **Solución:** Usar `application.yml` con valores neutros, más `application-local.yml` (H2, consola, `data.sql`) y `application-prod.yml` (PostgreSQL, `ddl-auto=validate`, consola deshabilitada, `spring.sql.init.mode=never`). Los secretos deben venir de variables de entorno o de un vault, y conviene declarar `spring.jpa.open-in-view=false`.
- **Test que lo demostraría:** Un test de arranque con `@ActiveProfiles("prod")` y Testcontainers que verifique que `spring.h2.console.enabled` es falso y que el esquema se valida contra las migraciones.

---

### H-018 — `@Data` en entidades JPA y modelo anémico
- **Ubicación:** `reto-a/src/main/java/co/proteccion/cis/retoa/domain/Aporte.java:12`, `Saldo.java:9` y `EventoAporte.java:11`.
- **Severidad:** Baja
- **Categoría:** Clean code / Persistencia
- **Evidencia:**
  ```java
  @Entity @Data @NoArgsConstructor @AllArgsConstructor
  public class Aporte { ... }
  ```
- **Motivo:** `@Data` genera `equals/hashCode` sobre todos los campos, incluido el `id` generado. El hash de una entidad cambia al persistirla, lo que rompe `Set` y `Map` y el comportamiento con proxies. También genera setters públicos para todo, y cualquier código puede hacer `saldo.setTotalMes(...)` saltándose el tope. Hoy no hay relaciones ni colecciones, así que el impacto inmediato es bajo. Se reporta porque es la puerta de entrada de los errores de H-002 y H-003 cuando el modelo crezca.
- **Solución:** Usar `@Getter` y un constructor protegido para JPA. Implementar `equals/hashCode` basados en el id con la convención de Hibernate, o no implementarlos. Mover el comportamiento al dominio (`SaldoMensual.acreditar(Monto)`, `Aporte.registrar(...)` como factory) y quitar los setters.
- **Test que lo demostraría:** Un test unitario del dominio que demuestre que no se puede llevar el saldo por encima del tope sin pasar por `acreditar`. En la práctica es una verificación de diseño, cubierta por los tests de H-002.

---

## 5. Preguntas abiertas

1. **¿El tope mensual es inclusivo?** ¿Un acumulado de exactamente 10.000.000 se acepta? El mensaje «supera el tope» sugiere que sí, pero el código lo rechaza.
2. **¿El tope tiene origen normativo o de producto,** y es por afiliado y mes calendario, por plan o por fondo? Define si el incumplimiento es regulatorio o comercial.
3. **¿El umbral de revisión (5 M) se evalúa por aporte o por acumulado?** Hoy es por aporte individual, de modo que tres aportes de 4,9 M en el mismo mes no se marcan. Si la marca tiene fines de prevención de lavado, el fraccionamiento la evade. No lo reporto como hallazgo porque la regla no está documentada. ¿Debe ser `>` o `>=`?
4. **¿Debe exponerse `marcadaRevision` al afiliado o canal?** Si es un control antifraude o de prevención de lavado, revelarlo al cliente puede ser indebido. Hay que confirmarlo con Cumplimiento.
5. **¿Cuál es la zona horaria y el corte de negocio?** ¿America/Bogota a medianoche? ¿Hay un horario de corte bancario distinto?
6. **¿Los montos en COP admiten decimales?** Si no, la escala debería ser 0 y `@Digits(fraction = 0)`.
7. **¿Quién es la fuente de verdad del afiliado y de su estado?** (activo, retirado, bloqueado). Hoy «existir» equivale a «tener una fila en `saldo`». ¿Hay que validar que el afiliado esté activo o que el fondo esté abierto?
8. **¿Quién crea la fila de saldo de cada mes?** ¿Un proceso batch o el primer aporte del mes?
9. **¿Hay un gateway que autentique?** ¿Qué actores pueden registrar aportes: el afiliado, un asesor o un recaudo masivo? Define si H-009 es Alta o Crítica.
10. **¿Cuál es el catálogo de canales válidos?**
11. **¿El «consolidado» debe devolver la lista de aportes o un total?** El nombre sugiere un total.
12. Se asume que el monto del aporte ya fue recaudado. ¿O el registro dispara un débito? Si dispara un débito, la idempotencia (H-008) pasa a ser Crítica.

## 6. Observaciones descartadas

- **Log con `afiliadoId` y monto** (`AporteService:64`): el `afiliadoId` es un identificador interno sintético, no un documento de identidad. Registrar montos a nivel INFO puede ser aceptable según la política de logs. La deficiencia real es que falta contexto y que el log se escribe antes del commit (H-016).
- **`@Value` para `topeMensual` y `umbralRevision`:** externalizar los parámetros es correcto. El problema es el tipo `double` (H-005). Lo ideal sería un `@ConfigurationProperties` validado, pero no es bloqueante.
- **`spring-boot-starter-jdbc` redundante con `data-jpa`:** es inocuo. Se elimina de forma natural al resolver H-007.
- **`open-in-view` activo por defecto:** no hay relaciones lazy, así que hoy no causa N+1 ni consultas en la vista. Se recomienda desactivarlo en H-017, pero no es un hallazgo.
- **`GenerationType.IDENTITY`:** es válido en PostgreSQL. El impacto en batch inserts no aplica a este volumen.
- **Inyección por constructor con `@RequiredArgsConstructor`:** es correcto. No hay inyección por campo en el código productivo; `@Autowired` en el test es aceptable.
- **No hay arquitectura hexagonal ni puertos:** la ausencia de un patrón no es un hallazgo en sí. Lo que sí lo es, porque tiene impacto concreto, es el controller accediendo a la BD (H-007) y el dominio acoplado a JPA y anémico (H-018).
- **`AporteRequest` con `@Data`:** en un DTO es inocuo. Un `record` sería más idiomático en Java 21.
- **Comentarios «en pesos colombianos»:** son correctos. No contradicen el código.

## 7. Plan de acción sugerido

**Bloque 1, bloqueantes del MR (hay que hacerlos antes de volver a revisar):**
1. **H-001 + H-007:** sacar el SQL del controller y consultar a través de un servicio o repositorio parametrizado, devolviendo un DTO. Cierra la inyección y restablece las capas.
2. **H-005:** introducir `Monto` (`BigDecimal`) en el DTO, el dominio, las entidades y la configuración. Es la base de los demás arreglos de dinero.
3. **H-004 + H-003:** `@Transactional` en el caso de uso más bloqueo (optimista con `@Version`, o pesimista o update atómico) sobre el saldo.
4. **H-006 + H-010:** saldo por `(afiliadoId, mes)` con el mes derivado de un `Clock` en la zona de negocio, calculado una sola vez.
5. **H-002:** regla de tope con `compareTo` dentro del dominio (`SaldoMensual.acreditar`).
6. **H-011:** tests que demuestren 1–5: frontera del tope, saldo = Σ aportes, concurrencia, rollback, inyección y corte de mes, contra PostgreSQL con Testcontainers.

**Bloque 2, antes de producción:**
7. **H-008:** idempotencia con `Idempotency-Key` y restricción única.
8. **H-009:** Resource Server JWT con autorización por afiliado (según la respuesta a la pregunta 9).
9. **H-015:** Flyway con restricciones y `ddl-auto=validate`, para respaldar en BD los puntos 3, 4 y 7.
10. **H-012 + H-013:** Bean Validation y `@RestControllerAdvice` con `ProblemDetail`, `201 Created` y versionado de la ruta.

**Bloque 3, deuda planificada:**
11. **H-014, H-016, H-017, H-018:** DTO de respuesta y paginación, auditoría con `aporteId`, actor y correlación, perfiles de configuración, y un modelo de dominio sin `@Data`.
