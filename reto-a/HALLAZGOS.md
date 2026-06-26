# Revisión de código — Módulo de aportes voluntarios (reto-a)

> Revisión tipo Merge Request. Severidad: **Crítica/Alta** bloquean el MR · **Media** corregir antes de producción · **Baja** mejora.

## Resumen de hallazgos

| # | Hallazgo | Ubicación | Severidad |
|---|----------|-----------|-----------|
| 1 | Inyección SQL en la consulta de consolidado | `AporteController.consolidado` | **Crítica** |
| 2 | Uso de `double` para representar dinero | Entidades, DTO y topes | **Crítica** |
| 3 | Control de tope incorrecto (`==` sobre `double`) | `AporteService.registrar` | **Crítica** |
| 4 | Escritura no transaccional (no atómica) | `AporteService.registrar` | **Crítica** |
| 5 | Condición de carrera en el saldo (read-modify-write) | `AporteService.registrar` | **Alta** |
| 6 | Sin idempotencia: reintentos duplican aportes y saldo | `AporteService` / `AporteController` | **Alta** |
| 7 | El saldo no se segmenta por mes; el tope nunca se reinicia | `Saldo` + `AporteService` | **Alta** |
| 8 | El controller salta servicio/repositorio (CQRS / Clean Arch) | `AporteController.consolidado` | **Alta** |
| 9 | Dominio acoplado a JPA + `@Data` en entidades | `domain/*` | **Media** |
| 10 | Sin validación de entrada ni manejo de errores HTTP | `AporteRequest`, `AporteController` | **Media** |
| 11 | Período derivado con zona horaria implícita | `AporteService.registrar` | **Media** |
| 12 | El evento de auditoría no enlaza al aporte | `EventoAporte` | **Media** |
| 13 | Configuración no apta para producción (H2/consola) | `application.properties` | **Media** |
| 14 | Se expone la entidad JPA en la respuesta del API | `AporteController` | **Baja** |
| 15 | PII financiera en logs | `AporteService` | **Baja** |

---

## Detalle de la revisión

### 1. Inyección SQL — **Crítica**
**Ubicación:** `controller/AporteController.java:38-44`

```java
String sql = "SELECT * FROM aporte WHERE afiliado_id = '"
        + afiliadoId + "' AND periodo = '" + periodo + "'";
```
**Problema:** parámetros del cliente concatenados al SQL. `periodo = 2025-06' OR '1'='1` expone los aportes de todos los afiliados.
**Corrección:** usar el repositorio existente `AporteJpaRepository.findByAfiliadoIdAndPeriodo(...)` (hoy sin usar) o, si se mantiene JDBC, consulta parametrizada con `?`.

### 2. `double` para dinero — **Crítica**
**Ubicación:** `Aporte.monto`, `Saldo.totalMes`, `EventoAporte.monto`, `AporteRequest.monto`, topes en `AporteService`.
**Problema:** el punto flotante binario acumula error de redondeo → descuadres y saldos que no cuadran. No aceptable en dominio financiero.
**Corrección:** `BigDecimal` + columnas `DECIMAL(19,2)`, redondeo explícito y comparaciones con `compareTo`.

### 3. Control de tope incorrecto — **Crítica**
**Ubicación:** `service/AporteService.java:45`

```java
if (nuevo == topeMensual) { throw new IllegalArgumentException("...supera el tope..."); }
```
**Problema:** usa `==` en vez de `>`. Solo bloquea el valor exacto y **deja pasar cualquier monto que supere el tope**. 
**Corrección:** comparar con `compareTo(...) > 0` (definiendo si el tope es inclusivo). Falta test del camino del tope (justo en / por encima / por debajo).

### 4. Escritura no transaccional — **Crítica**
**Ubicación:** `service/AporteService.java:33-67`
**Problema:** tres `save` independientes sin `@Transactional`. Si falla uno, el saldo queda incrementado sin aporte registrado.
**Corrección:** `@Transactional` para que las tres escrituras sean atómicas si alguna falla se haga rollback de lo que vaya hasta ese momento.

### 5. Condición de carrera al actualizar el saldo — **Alta**
**Ubicación:** `service/AporteService.java:40-50`
**Problema:**
El saldo mensual del afiliado se consulta, se calcula el nuevo valor y luego se guarda. Si dos solicitudes para el mismo afiliado llegan casi al mismo tiempo, ambas pueden leer el mismo saldo antes de que alguna lo actualice.

Como consecuencia, ambas operaciones se ejecutan con información desactualizada, lo que puede provocar que:
- se pierda una actualización del saldo (*lost update*);
- se registren aportes que excedan el tope mensual permitido;
- el saldo almacenado en la base de datos quede inconsistente.

**Ejemplo:**
Saldo actual: $9.000.000 (tope: $10.000.000)

Dos solicitudes de $800.000 llegan simultáneamente:
- Solicitud A lee $9.000.000.
- Solicitud B también lee $9.000.000.
- Ambas validan que el aporte es permitido.
- Ambas actualizan el saldo.

El sistema termina aceptando $1.600.000 en aportes, aunque solo debía aceptar uno.
**Corrección:**
Proteger la actualización del saldo mediante control de concurrencia, por ejemplo:
- bloqueo optimista (`@Version`);
- bloqueo pesimista (`@Lock(PESSIMISTIC_WRITE)`);
- una actualización atómica directamente en la base de datos (`UPDATE ... WHERE ...`).

### 6. Falta de idempotencia — **Alta**
**Ubicación:** `AporteService.registrar` / `AporteController.registrar`
**Problema:** sin clave de idempotencia, un reintento por timeout registra el aporte y suma al saldo dos veces (doble cargo).
**Corrección:** `Idempotency-Key` persistida con restricción única; ante clave repetida devolver el resultado previo sin reaplicar.

### 7. El saldo mensual no se controla por período — Alta
**Ubicación:** `domain/Saldo.java`, `service/AporteService.java:40,52`, `data.sql`

**Problema:**
El saldo se consulta únicamente por `afiliadoId`, sin considerar el período (mes). Aunque la entidad `Saldo` tiene el campo `mes`, este nunca se utiliza ni se asigna, por lo que el saldo se acumula de forma indefinida y el tope mensual deja de aplicarse correctamente al iniciar un nuevo mes.

**Corrección:**
Gestionar el saldo por período utilizando una clave única `(afiliadoId, periodo)` y realizar la creación o actualización (*upsert*) del registro correspondiente al mes en curso.

### 8. El controller salta servicio/repositorio — **Alta**
**Ubicación:** `controller/AporteController.java:19,38-44`
**Problema:** inyecta `JdbcTemplate` y arma SQL en la capa de presentación; rompe Clean Architecture y es el vector del Hallazgo 1.
**Corrección:** delegar la lectura a un servicio/handler de consulta que use el repositorio JPA.

### 9. Dominio acoplado a JPA + `@Data` — **Media**
**Ubicación:** `domain/*`
**Problema:** el dominio depende de `jakarta.persistence` y Lombok. `@Data` en `@Entity` genera `equals/hashCode` sobre el `@Id` autogenerado (problemas con Hibernate) y setters públicos que rompen invariantes del saldo.
**Corrección:** separar dominio de persistencia; `equals/hashCode` por identidad de negocio; encapsular la mutación del saldo.

### 10. Sin validación ni manejo de errores HTTP — **Media**
**Ubicación:** `AporteRequest`, `AporteController.registrar`
**Problema:** sin `@Valid`/Bean Validation; las reglas lanzan `IllegalArgumentException` → el cliente recibe **HTTP 500** en errores que son 400/404/409.
**Corrección:** Bean Validation en el DTO + `@RestControllerAdvice` que mapee excepciones de dominio a códigos HTTP.

### 11. Período con zona horaria implícita — **Media**
**Ubicación:** `service/AporteService.java:52,57`
**Problema:** `LocalDate.now()` usa la zona de la JVM; cerca del cambio de mes imputa el aporte al período equivocado. Se invoca `now()` dos veces.
**Corrección:** `Clock` inyectable con zona explícita (`America/Bogota`), calculado una sola vez.

### 12. El evento de auditoría no enlaza al aporte — **Media**
**Ubicación:** `domain/EventoAporte.java:27-32`, `service/AporteService.java:62`
**Problema:** el evento se crea antes de persistir el aporte (`id == null`) y no guarda `aporteId` ni el saldo resultante → sin trazabilidad.
**Corrección:** registrar `aporteId`, saldo antes/después y metadata; emitir tras el commit.

### 13. Configuración no apta para producción — **Media**
**Ubicación:** `application.properties`
**Problema:** H2 en memoria + `ddl-auto=create-drop` (se pierde todo al reiniciar) y `h2-console` habilitada (superficie de ataque). Sin perfiles ni migraciones versionadas.
**Corrección:** H2 solo en `test`; BD real + Flyway y `ddl-auto=validate` en producción; consola deshabilitada fuera de dev.

### 14. Se expone la entidad JPA en la respuesta — **Baja**
**Ubicación:** `AporteController.registrar` y `consolidado`
**Problema:** devuelve la entidad `Aporte`, acoplando el contrato del API al esquema interno.
**Corrección:** devolver un DTO de respuesta mapeado.

### 15. PII financiera en logs — **Baja**
**Ubicación:** `service/AporteService.java:64`
**Problema:** loguea `afiliadoId` y `monto` en texto plano (habeas data / datos financieros).
**Corrección:** enmascarar/tokenizar y registrar un id de transacción no sensible.

---

## Evidencia (validación sobre los endpoints)

Pruebas en vivo contra `POST /api/aportes` y `GET /api/aportes/consolidado`:

| Prueba | Resultado | Confirma |
|--------|-----------|----------|
| `GET /consolidado?afiliadoId=zzz' OR '1'='1` | Devuelve registros de otros afiliados | #1 |
| POST monto `20000000` (saldo 0,5M, tope 10M) | **HTTP 200, aceptado** | #3 |
| Monto serializado en respuesta | `"monto":2.0E7` (notación `double`) | #2 |
| Mismo payload enviado 3 veces | 3 aportes creados, saldo triplicado | #6 |
| POST asigna `periodo=2026-06`; `GET ...periodo=2025-06` (data.sql) | Devuelve `[]` pese a existir el aporte | #7, #11 |
| Monto negativo / afiliado inexistente / sin `monto` / body `{}` | Todos **HTTP 500** (debería 400/404) | #10 |
| `GET /h2-console` | **HTTP 302** (consola accesible) | #13 |

> Nota sobre #3: el `==` hace el control **inalcanzable** — solo se dispararía si el acumulado cayera *exactamente* en `10000000.0`; en la práctica el tope nunca rechaza.

---

## Qué bloquea el MR

No aprobaría hasta resolver, en orden: **1 (SQLi) → 3 (tope con `==`) → 2 (`double`) → 4 y 5 (atomicidad + concurrencia) → 6 (idempotencia)**. Los hallazgos 7 y 8 deben entrar en el mismo MR.

**Tests:** la suite solo cubre caminos felices; no hay pruebas de tope, concurrencia, idempotencia ni reinicio mensual — justo las reglas más sensibles.
