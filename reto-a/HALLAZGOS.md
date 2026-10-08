# Reto A · Hallazgos de la auditoría

Revisión del módulo `reto-a` (registro de aportes a un fondo voluntario) como si fuera un Merge Request de la célula.
Los hallazgos están priorizados por lo que un revisor **bloquearía antes de aprobar**. Contexto: el código manipula
dinero y corre en un entorno regulado por la SFC.

| Severidad | Hallazgos |
|---|---|
| 🔴 Crítica (bloquean el MR) | 1 – 6 |
| 🟠 Alta | 7 – 10 |
| 🟡 Media | 11 – 15 |
| 🟢 Baja | lista final |

---

## 🔴 Críticos: bloquean el MR

### 1. Inyección SQL en el consolidado
**Ubicación:** `AporteController.java:41-42`

- **Problema:** la consulta se arma pegando `afiliadoId` y `periodo` directamente en el texto del SQL. Con
  `afiliadoId = x' OR '1'='1` se pueden leer los aportes de todos los afiliados. Es OWASP A03.
- **Corrección:** usar una consulta parametrizada
  (`jdbc.query("... WHERE afiliado_id = ? AND periodo = ?", mapper, afiliadoId, periodo)`) o, mejor, un método del repositorio.

### 2. El tope mensual está al revés
**Ubicación:** `AporteService.java:45`, `if (nuevo == topeMensual)`

- **Lo que verifiqué:** AF-003 tiene 4,5M en el seed. Si aporta 6M queda en 10,5M y el aporte se acepta. Un aporte de 20M
  también se acepta. El único caso que se rechaza es llegar exactamente a 10M, que justamente es un valor válido.
- **Problema:** en la práctica no hay ningún tope. Además, comparar `double` con `==` es frágil por sí solo.
- **Corrección:** `if (nuevo.compareTo(tope) > 0)` usando `BigDecimal`.
- **Para el MR:** el bug pasó porque no hay un test del tope. Vale la pena decirlo en la revisión.

### 3. Dinero en `double`
**Ubicación:** `Aporte`, `Saldo`, `EventoAporte`, `AporteRequest` y el `RowMapper`

- **Lo que verifiqué:** sumar 10 veces 0.1 da `0.9999999999999999`.
- **Problema:** en un saldo acumulado esos errores se van sumando, y la SFC exige exactitud.
- **Corrección:** usar `BigDecimal` con escala fija y un `RoundingMode` explícito. En la base, `NUMERIC(19,2)`, o
  `NUMERIC(19,0)` si se trabaja en pesos enteros.

### 4. No hay transacción
**Ubicación:** `AporteService.registrar`

- **Problema:** el método hace tres escrituras separadas: actualiza el saldo, guarda el evento y guarda el aporte. Si falla la
  última, el saldo ya subió y queda un evento de un aporte que no existe.
- **Corrección:** `@Transactional` en el caso de uso. Si el evento es para integración, usar un outbox dentro de la
  misma transacción.

### 5. Condición de carrera en el saldo
**Ubicación:** `AporteService.java:40-50`

- **Problema:** el método lee el saldo, le suma el monto y lo guarda, sin ningún bloqueo. Si llegan dos aportes
  concurrentes, uno de los dos se pierde (*lost update*), y entre ambos pueden pasar el tope aunque cada uno por separado
  esté por debajo.
- **Corrección:** cualquiera de estas tres:
  - `@Version` en `Saldo` (bloqueo optimista) y reintentar si choca.
  - `@Lock(PESSIMISTIC_WRITE)`, es decir `SELECT ... FOR UPDATE`.
  - Un `UPDATE saldo SET total = total + :m WHERE ... AND total + :m <= :tope` atómico, revisando cuántas filas afectó.

### 6. No es idempotente
- **Problema:** si el cliente reintenta la misma solicitud (timeout, doble clic), el aporte queda registrado dos veces. El
  propio brief pide que la operación sea idempotente.
- **Corrección:** recibir un header `Idempotency-Key` o un id de solicitud del cliente, guardarlo con restricción `UNIQUE` y,
  si ya existe, devolver el aporte que ya se había creado.

---

## 🟠 Altos

### 7. El saldo no es mensual de verdad
- `Saldo` tiene el campo `mes`, pero `findByAfiliadoId` no filtra por mes. Resultado: el acumulado nunca se reinicia.
- El día que un afiliado tenga dos meses guardados, la consulta revienta con `IncorrectResultSizeDataAccessException`.
- El seed usa `2025-06`, pero el periodo se calcula con `LocalDate.now()`, así que no cuadran.
- Además, que no exista fila de saldo hace que se responda "afiliado no encontrado", lo cual mezcla dos conceptos distintos.
- **Corrección:** buscar con `findByAfiliadoIdAndMes`, crear el saldo del mes si no existe y validar la existencia del
  afiliado por separado.

### 8. Seguridad de los endpoints
- **Problema:** no hay autenticación ni autorización. Cualquiera puede consultar o registrar aportes de cualquier afiliado
  (IDOR, OWASP A01).
- **Corrección:** agregar Spring Security (OAuth2/JWT) y validar que quien llama tiene derecho sobre ese `afiliadoId`.

### 9. Consola H2 expuesta
**Ubicación:** `application.properties`

- **Problema:** está habilitada con el usuario `sa` y sin contraseña, lo que da acceso directo a la base.
- **Corrección:** quitarla o dejarla solo en un perfil `dev`. Las credenciales van por variables de entorno o secretos.

### 10. Errores y validación de entrada
- **Problema:** no hay `@Valid` ni `@NotBlank`, `canal` es texto libre (puede llegar `null` o cualquier cosa) y el brief pide
  fecha, pero el request no la recibe.
- **Problema:** las `IllegalArgumentException` salen como HTTP 500 en vez de 400 o 422.
- **Corrección:** usar Bean Validation, un enum `Canal` y un `@RestControllerAdvice` con respuestas `ProblemDetail`.

---

## 🟡 Medios

### 11. Se rompe la arquitectura (Clean Architecture y CQRS)
- El controller usa `JdbcTemplate` directamente y se salta el servicio y el repositorio.
- Las entidades JPA hacen de modelo de dominio y además se devuelven en la API, así que se expone el modelo interno.
- Las reglas de negocio (monto positivo, tope, umbral) están en el servicio y no en el dominio.
- No hay puertos: el servicio depende de interfaces de Spring Data.
- **Corrección:** separar dominio, aplicación e infraestructura. Usar un caso de uso de comando (`RegistrarAporte`) y una
  consulta separada (`ConsultarConsolidado`) que devuelvan DTOs de respuesta.

### 12. Trazabilidad del evento
**Ubicación:** `EventoAporte`

- **Problema:** el evento se crea antes de guardar el aporte, así que no tiene el `aporteId`. Además usa `LocalDateTime`
  sin zona horaria.
- **Corrección:** crear el evento después de guardar el aporte, guardar el id y usar `Instant` u `OffsetDateTime`. Para
  la SFC conviene registrar también quién hizo la operación y el canal.

### 13. Zona horaria
- **Problema:** `LocalDate.now()` usa la zona del servidor, que en K8s suele ser UTC. Un aporte hecho a las 11 p. m. en
  Bogotá el último día del mes queda en el mes siguiente.
- **Corrección:** inyectar un `Clock` con `America/Bogota`, que además vuelve el código testeable.

### 14. Los tests no protegen nada
- No hay test del tope, ni de los límites (¿un monto exactamente igual al umbral se marca?), ni de concurrencia, ni de
  idempotencia.
- Los tests comparten estado: el saldo de AF-001 cambia entre un test y otro y no hay rollback, así que dependen del orden
  en que corran.
- Comparan `double` con `assertEquals` sin tolerancia.

### 15. Logs con datos financieros
**Ubicación:** `AporteService.java:64`

- **Problema:** se loguean monto y afiliado. Además, el mensaje de error repite el id que llegó, lo que permite enumerar
  afiliados y hacer *log injection*.
- **Corrección:** loguear un id de correlación y enmascarar los datos sensibles.

---

## 🟢 Bajos

- **Consolidado incompleto:** devuelve solo la lista de aportes; el brief pide total más detalle.
- **POM:** `starter-jdbc` sobra porque ya viene con JPA, y falta `starter-validation`.
- **Esquema:** se usa H2 con `ddl-auto=create-drop` cuando el stack es PostgreSQL. Debería manejarse con Flyway o Liquibase.
- **API:** el POST devuelve 200 en vez de 201 con `Location`.
- **Lombok en entidades:** `@Data` en entidades JPA genera `equals`/`hashCode` problemáticos.
- **RowMapper:** lanza NPE si `fecha` llega `null`.
