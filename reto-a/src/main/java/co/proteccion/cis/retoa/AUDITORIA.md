# Auditoría de código — Módulo de aportes a fondo voluntario (Reto A)

**Autor:** Felipe Cano
**Contexto:** Revisión como Merge Request antes de aprobación, bajo estándares CIS (Clean Architecture, SOLID, CQRS) y entorno regulado por la SFC.

## Resumen ejecutivo

El módulo compila y los tests felices pasan, pero tiene **5 hallazgos críticos** que un revisor de célula bloquearía antes de aprobar el MR: manejo incorrecto de dinero con `double`, un bug de comparación que permite superar el tope mensual, ausencia total de idempotencia, una condición de carrera en la actualización de saldo, y una inyección SQL explotable en el endpoint de consulta. A esto se suman hallazgos de severidad alta y media relacionados con separación de capas (CQRS) y trazabilidad de auditoría.

## Tabla de hallazgos

| # | Ubicación | Severidad | Hallazgo |
|---|-----------|-----------|----------|
| 1 | `AporteController.java` L41-43 | **Crítica** | Inyección SQL en `consolidado()` |
| 2 | `AporteService.java` L45 | **Crítica** | Comparación `==` deja pasar montos que superan el tope |
| 3 | `Aporte.java`, `AporteRequest.java`, `AporteService.java` | **Crítica** | No existe mecanismo de idempotencia |
| 4 | `Saldo.java`, `AporteService.java` L40-50 | **Crítica** | Condición de carrera (lost update) en actualización de saldo |
| 5 | `Aporte.java`, `Saldo.java`, `AporteRequest.java` | **Crítica** | Uso de `double` para representar dinero |
| 6 | `AporteController.java` L19, L41-44 | Alta | Controller con acceso directo a datos, viola CQRS/Clean Architecture |
| 7 | `EventoAporte.java` | Media | Registro de auditoría no referencia el `Aporte` que lo originó |
| 8 | `application.properties` | Media | Consola H2 habilitada sin segregación de perfiles |
| 9 | `AporteRequest.java` (campo `canal`) | Baja | Sin validación/whitelist de valores permitidos |
| 10 | `AporteService.java` L57 | Baja | Fecha del aporte siempre `LocalDate.now()`, sin decisión documentada sobre fecha efectiva vs. fecha de registro |

---

## Detalle por hallazgo

### 1. Inyección SQL — `AporteController.java`, líneas 41-43
**Severidad:** Crítica
**Por qué es un problema:** el endpoint `consolidado()` concatena `afiliadoId` y `periodo` directamente en un string SQL ejecutado con `JdbcTemplate`. Cualquier valor de query param llega sin sanitizar al motor de base de datos. En un sistema financiero regulado esto es una vulnerabilidad OWASP Top 10 (A03:2021 — Injection) que permite lectura, modificación o exfiltración de datos de aportes de todos los afiliados, no solo del que hace la consulta.
**Cómo lo corregiría:** eliminar el `JdbcTemplate` manual y usar el `AporteJpaRepository` existente (`findByAfiliadoIdAndPeriodo`), que ya usa parámetros bindeados. Si se necesita SQL nativo por alguna razón, usar `PreparedStatement`/`@Query` con parámetros nombrados, nunca concatenación de strings.

### 2. Comparación `==` en la validación del tope mensual — `AporteService.java`, línea 45
**Severidad:** Crítica
**Por qué es un problema:** `if (nuevo == topeMensual)` solo bloquea si el acumulado cae exactamente en el valor del tope. Cualquier aporte que lo supere (por ejemplo, pasar de 9.500.000 a 12.000.000 con un tope de 10.000.000) se acepta sin problema, porque `12000000 == 10000000` es falso. La regla de negocio "existe un tope mensual por afiliado" queda, en la práctica, sin aplicar en el caso general.
**Cómo lo corregiría:** cambiar la condición a `nuevo > topeMensual` (o `>=` si el tope es inclusive, a confirmar con negocio), y agregar un test de borde que cubra exactamente ese escenario.

### 3. Ausencia de idempotencia — `Aporte.java`, `AporteRequest.java`, `AporteService.registrar()`
**Severidad:** Crítica
**Por qué es un problema:** el enunciado exige explícitamente que el registro de un aporte sea idempotente, pero no existe ningún campo (idempotency key, hash de negocio, o similar) que permita al servicio distinguir un reintento legítimo (timeout de red, doble clic, reintento de un gateway de pagos) de un aporte nuevo. Hoy, dos requests idénticos generan dos aportes y duplican dinero real en el saldo del afiliado.
**Cómo lo corregiría:** agregar un campo `idempotencyKey` (UUID provisto por el cliente, o generado determinísticamente a partir de afiliado+monto+fecha+canal+origen) con una restricción de unicidad a nivel de base de datos, y en el service verificar existencia antes de procesar, devolviendo el resultado ya registrado si la clave se repite en vez de crear un nuevo registro.

### 4. Condición de carrera en la actualización de saldo — `Saldo.java`, `AporteService.java` líneas 40-50
**Severidad:** Crítica
**Por qué es un problema:** el flujo es un read-modify-write clásico: se lee el `Saldo`, se calcula en memoria (`s.getTotalMes() + monto`), y se guarda. No hay `@Transactional` en el método del service, ni control de concurrencia optimista (`@Version`) ni bloqueo pesimista en `Saldo`. Si dos aportes del mismo afiliado llegan casi simultáneamente (dos pestañas, un reintento en paralelo, un batch), ambos leen el mismo saldo inicial y el segundo `save()` sobrescribe al primero: un aporte se "pierde" del acumulado mensual, lo que además rompe la validación del tope, porque esta se calcula sobre un saldo desactualizado.
**Cómo lo corregiría:** envolver `registrar()` en `@Transactional`, agregar `@Version` a `Saldo` para optimistic locking (y manejar `OptimisticLockException` con reintento o error controlado), o alternativamente mover el cálculo del acumulado a una operación atómica a nivel de base de datos (`UPDATE saldo SET total_mes = total_mes + ? WHERE afiliado_id = ?`) seguida de una relectura para validar el tope.

### 5. Uso de `double` para dinero — `Aporte.java`, `Saldo.java`, `AporteRequest.java`
**Severidad:** Crítica
**Por qué es un problema:** `double` es punto flotante binario; no puede representar exactamente la mayoría de valores decimales, lo que produce errores de redondeo acumulativos en sumas repetidas. En un fondo regulado por la SFC, donde la corrección numérica del dinero no es negociable, esto es inaceptable incluso si hoy los tests no lo evidencian (los montos de prueba son "redondos").
**Cómo lo corregiría:** cambiar `monto`, `totalMes`, `topeMensual` y `umbralRevision` a `BigDecimal`, con una `scale` y `RoundingMode` explícitos y consistentes en toda la capa de dominio, y mapear la columna en base de datos como `NUMERIC`/`DECIMAL` en vez de `DOUBLE`.

### 6. Controller con acceso directo a datos — `AporteController.java`, líneas 19, 41-44
**Severidad:** Alta
**Por qué es un problema:** el controller inyecta un `JdbcTemplate` en paralelo al `AporteService`, es decir, la capa de presentación tiene una vía de acceso directa a la base de datos que evita por completo la capa de dominio/servicio. Esto rompe la separación comando/consulta que pide el CIS (CQRS): no hay un punto único donde se apliquen reglas de negocio, logging o validaciones para las consultas, y dificulta testear el controller de forma aislada.
**Cómo lo corregiría:** crear un `AporteQueryService` (o método de consulta en el service existente) que use el repositorio JPA, y que el controller dependa únicamente de la capa de servicio, nunca de infraestructura de persistencia.

### 7. Trazabilidad incompleta del evento de auditoría — `EventoAporte.java`
**Severidad:** Media
**Por qué es un problema:** `EventoAporte` guarda afiliado, monto, tipo y fecha del evento, pero no referencia el `id` del `Aporte` que lo originó ni el canal. En un entorno regulado, un log de auditoría que no permite reconstruir de forma inequívoca a qué aporte corresponde cada evento es débil frente a un requerimiento de trazabilidad de la SFC.
**Cómo lo corregiría:** agregar `aporteId` (FK) y `canal` a `EventoAporte`, y considerar si el evento debería persistirse antes/junto con el aporte dentro de la misma transacción para garantizar consistencia.

### 8. Consola H2 habilitada sin segregación de perfiles — `application.properties`
**Severidad:** Media
**Por qué es un problema:** `spring.h2.console.enabled=true` es razonable para desarrollo/pruebas, pero no hay evidencia de un perfil (`application-dev.properties` vs `application-prod.properties`) que garantice que esta configuración nunca llegue a un ambiente real. Es un patrón de riesgo típico: lo que hoy es cómodo para auditar el esquema puede terminar expuesto en producción por descuido de configuración.
**Cómo lo corregiría:** mover esta configuración a un perfil `dev`/`test` explícito y verificar en el pipeline de GitLab CI/CD que el perfil `prod` nunca la incluya.

### 9. Sin validación del campo `canal` — `AporteRequest.java`
**Severidad:** Baja
**Por qué es un problema:** `canal` es un `String` libre; acepta cualquier valor, lo que puede ensuciar reportes y análisis posteriores, y no se corresponde con un dominio controlado de canales de origen (app móvil, web, oficina, etc.).
**Cómo lo corregiría:** convertir `canal` a un `enum` con los canales válidos del negocio, validado en el DTO de entrada.

### 10. Fecha de aporte sin decisión documentada — `AporteService.java`, línea 57
**Severidad:** Baja
**Por qué es un problema:** el enunciado pide registrar "fecha" como parte del aporte, pero el servicio siempre usa `LocalDate.now()` del servidor, sin permitir (o explícitamente rechazar) una fecha efectiva distinta enviada por el cliente. No es necesariamente incorrecto, pero es una decisión de diseño que hoy no está documentada ni es intencional en el código.
**Cómo lo corregiría:** decidir explícitamente si la fecha es siempre server-side (recomendado para evitar manipulación) y, si es así, dejar claro en el DTO que el cliente no debe (o no puede) enviarla, en vez de que sea simplemente un campo ausente por omisión.

---

## Priorización — qué bloquearía el MR

**Bloquean la aprobación (crítico, no negociable):**
1. Inyección SQL (#1) — vulnerabilidad explotable hoy.
2. Bug del tope mensual con `==` (#2) — la regla de negocio central no se aplica.
3. Falta de idempotencia (#3) — requisito explícito del enunciado, ausente.
4. Condición de carrera en saldo (#4) — pérdida real de dinero en concurrencia.
5. `double` para dinero (#5) — corrección numérica no negociable en este dominio.

**Debería resolverse antes de producción, pero no necesariamente bloquea este MR puntual si hay un ticket de seguimiento:**
6. Separación CQRS en el controller (#6).
7. Trazabilidad del evento de auditoría (#7).

**Mejoras de higiene, no bloqueantes:**
8. Perfil de H2 console (#8), validación de `canal` (#9), decisión sobre fecha (#10).

## Nota sobre cobertura de pruebas

Los tests existentes cubren únicamente camino feliz, umbral de revisión y dos validaciones negativas simples. No hay pruebas de: el caso borde del tope mensual (que habría detectado el hallazgo #2), concurrencia, ni idempotencia. Antes de aprobar el MR pediría al menos un test que reproduzca el bug del tope y uno que simule una request duplicada.