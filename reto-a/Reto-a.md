# Reto A — Hallazgos de auditoría

Módulo: `reto-a` · Servicio de registro de aportes voluntarios  
Revisado como Merge Request · Contexto: entorno financiero regulado (SFC)

---

## Resumen ejecutivo

El módulo compila y los tests felices pasan, pero **no es mergeable** en su estado actual. Hay defectos de corrección monetaria, concurrencia e idempotencia que permiten inconsistencias contables, además de una vulnerabilidad de inyección SQL y ausencia total de controles de acceso. Los hallazgos están ordenados por lo que bloquearía la aprobación del MR.

---

## Hallazgos bloqueantes

### H-01 · Validación del tope mensual incorrecta (operador `==` en lugar de `>`)

| Campo | Detalle |
|-------|---------|
| **Ubicación** | `AporteService.java`, líneas 43–47 |
| **Severidad** | **Crítica** |

**Por qué es un problema**

La condición `if (nuevo == topeMensual)` solo rechaza cuando el acumulado queda *exactamente* en el tope. Cualquier monto que lo supere pasa sin validación.El problema no es solo el operador, sino el uso de double que hace la comparación insegura.

Ejemplo con `AF-003` (saldo inicial 4.500.000, tope 10.000.000): un aporte de 6.000.000 deja un acumulado de 10.500.000 y **no se rechaza**. Además, un aporte que lleve el acumulado *exactamente* al tope sí se rechaza, cuando debería ser válido.

```java
if (nuevo == topeMensual) {  // bug: debería ser nuevo > topeMensual
    throw new IllegalArgumentException("El monto supera el tope mensual permitido");
}
```

**Corrección propuesta**

Usar `BigDecimal` para la comparación y rechazar cuando `nuevo.compareTo(topeMensual) > 0`. Agregar test con `AF-003` que supere el tope y otro que llegue exactamente al tope.

---

### H-02 · Inyección SQL en endpoint de consulta

| Campo | Detalle |
|-------|---------|
| **Ubicación** | `AporteController.java`, líneas 38–44 |
| **Severidad** | **Crítica** |

**Por qué es un problema**

Los parámetros `afiliadoId` y `periodo` se concatenan directamente en la consulta SQL. Un atacante puede inyectar SQL arbitrario vía query params.

```java
String sql = "SELECT * FROM aporte WHERE afiliado_id = '"
        + afiliadoId + "' AND periodo = '" + periodo + "'";
```

**Corrección propuesta**

Eliminar el `JdbcTemplate` del controller. Usar el método ya existente en `AporteJpaRepository.findByAfiliadoIdAndPeriodo()` o, si se mantiene JDBC, parametrizar con `?` y `PreparedStatement`. Mover la consulta a la capa de aplicación/infraestructura (CQRS — lado query).

---

### H-03 · Condición de carrera en actualización del saldo mensual

| Campo | Detalle |
|-------|---------|
| **Ubicación** | `AporteService.java`, líneas 40–50 · entidad `Saldo.java` |
| **Severidad** | **Crítica** |

**Por qué es un problema**

El flujo es read-modify-write sin bloqueo optimista ni pesimista:

1. Dos requests concurrentes leen el mismo `totalMes`.
2. Cada uno suma su monto y persiste.
3. Una actualización se pierde → el saldo queda subcontabilizado y el tope mensual puede eludirse.

En un sistema de aportes con tráfico real esto es un defecto de integridad financiera.

**Corrección propuesta**

- Agregar `@Version` en `Saldo` para bloqueo optimista y manejar `OptimisticLockException` con reintento o error 409.
- Alternativa: `UPDATE saldo SET total_mes = total_mes + :monto WHERE afiliado_id = :id AND total_mes + :monto <= :tope` en una sola sentencia atómica.
- Anotar el método con `@Transactional`.

---

### H-04 · Sin idempotencia en registro de aportes

| Campo | Detalle |
|-------|---------|
| **Ubicación** | `AporteService.registrar()`, `AporteRequest.java`, `AporteController.java` |
| **Severidad** | **Crítica** |

**Por qué es un problema**

No existe clave de idempotencia (`Idempotency-Key` o campo en el request). Un reintento de red, doble clic o replay del cliente crea aportes duplicados y suma el monto dos veces al saldo. En pagos/aportes regulados esto es inaceptable.

**Corrección propuesta**

- Recibir `idempotencyKey` (UUID) en header o body.
- Tabla o índice único `(idempotency_key)` / `(afiliado_id, idempotency_key)`.
- Si la clave ya existe, devolver el aporte original (200) sin reprocesar.
- Documentar contrato en OpenAPI.

---

### H-05 · Uso de `double` para montos monetarios

| Campo | Detalle |
|-------|---------|
| **Ubicación** | `Aporte.java:24`, `Saldo.java:20`, `EventoAporte.java:21`, `AporteRequest.java:15`, `AporteService.java:27–31,34,43` |
| **Severidad** | **Crítica** |

**Por qué es un problema**

`double` tiene errores de precisión binaria (p. ej. `0.1 + 0.2 ≠ 0.3`). En un contexto SFC donde se manejan pesos colombianos, la corrección numérica es requisito. Acumular muchos aportes con `double` puede producir centavos de diferencia respecto al ledger real.

**Corrección propuesta**

Migrar todos los montos a `BigDecimal` con escala 2 y `RoundingMode.HALF_UP`. Mapear en JPA con `@Column(precision = 19, scale = 2)`. Validar con `@Digits(integer = 17, fraction = 2)`.

---

### H-06 · Transacción ausente e inconsistencia ante fallos parciales

| Campo | Detalle |
|-------|---------|
| **Ubicación** | `AporteService.registrar()`, líneas 49–66 |
| **Severidad** | **Alta** |

**Por qué es un problema**

El método realiza tres escrituras sin `@Transactional`:

1. `saldoRepo.save(s)` — saldo actualizado
2. `eventoRepo.save(...)` — evento persistido
3. `aporteRepo.save(aporte)` — aporte persistido

Si el paso 3 falla, el saldo ya quedó incrementado y el evento ya existe, pero no hay aporte registrado. Estado inconsistente e irreconciliable sin intervención manual.

**Corrección propuesta**

Anotar `registrar()` con `@Transactional(rollbackFor = Exception.class)`. Considerar outbox pattern o evento de dominio publicado *después* del commit del aporte.

---

### H-07 · Sin autenticación ni autorización

| Campo | Detalle |
|-------|---------|
| **Ubicación** | Proyecto completo — ausencia de Spring Security |
| **Severidad** | **Crítica** |

**Por qué es un problema**

Cualquier actor puede registrar aportes y consultar consolidados sin identidad ni permisos. En un entorno regulado no hay trazabilidad de quién ejecutó la operación ni control de acceso por rol/canal.

**Corrección propuesta**

Integrar Spring Security (OAuth2/JWT o mTLS interno). Endpoints de escritura restringidos a roles autorizados. Propagar identidad del operador al log de auditoría.

---

## Hallazgos de alta severidad

### H-08 · El saldo no se valida contra el periodo actual

| Campo | Detalle |
|-------|---------|
| **Ubicación** | `AporteService.java:40–41,52–59` · `Saldo.java:23` |
| **Severidad** | **Alta** |

**Por qué es un problema**

El servicio busca saldo por `afiliadoId` pero no verifica que `Saldo.mes` coincida con el mes corriente (`yyyy-MM`). Si el registro de saldo quedó del mes anterior (job de cierre no corrido), los aportes se acumulan sobre un periodo incorrecto. El campo `periodo` del aporte se calcula con `LocalDate.now()` pero el saldo puede ser de otro mes.

**Corrección propuesta**

Validar `s.getMes().equals(periodoActual)` o resetear/crear saldo del mes al registrar. Fallar con error de negocio claro si el saldo está desactualizado.

---

### H-09 · Violación de separación comando/consulta (CQRS) y Clean Architecture

| Campo | Detalle |
|-------|---------|
| **Ubicación** | `AporteController.java` (POST vía service, GET vía `JdbcTemplate` directo) |
| **Severidad** | **Alta** |

**Por qué es un problema**

El comando pasa por `AporteService` + JPA; la consulta hace bypass con SQL crudo en el controller. Esto rompe la separación de responsabilidades del CIS, duplica acceso a datos, dificulta testear y fue el vector del H-02. Existe `AporteJpaRepository.findByAfiliadoIdAndPeriodo()` que no se usa.

**Corrección propuesta**

Extraer `ConsultarConsolidadoUseCase` (query side). El controller solo delega a casos de uso. Repositorio de lectura separado si se requiere proyección distinta del agregado de escritura.

---

### H-10 · Entidad de dominio expuesta directamente en la API REST

| Campo | Detalle |
|-------|---------|
| **Ubicación** | `AporteController.java:34–36,38–44` — retorna `Aporte` (entidad JPA) |
| **Severidad** | **Alta** |

**Por qué es un problema**

Acopla el contrato HTTP al modelo de persistencia. Cambios en la entidad (campos internos, relaciones) rompen clientes. En Clean Architecture la capa web debe usar DTOs/Response objects.

**Corrección propuesta**

Crear `AporteResponse` y `ConsolidadoResponse`. Mapear en el controller o con un mapper dedicado.

---

### H-11 · Sin validación de entrada en el request

| Campo | Detalle |
|-------|---------|
| **Ubicación** | `AporteRequest.java`, `AporteController.java:34` |
| **Severidad** | **Alta** |

**Por qué es un problema**

No hay `@Valid`, `@NotBlank`, `@NotNull`, `@Positive` ni whitelist de `canal`. Un body con `afiliadoId: null` o `canal: null` puede llegar al servicio y fallar con `NullPointerException` o persistir datos incompletos.

**Corrección propuesta**

Agregar Bean Validation al DTO y `@Valid` en el controller. Definir enum o lista cerrada de canales permitidos (`APP_MOVIL`, `WEB`, etc.).

---

### H-12 · Cobertura de tests insuficiente para reglas de negocio críticas

| Campo | Detalle |
|-------|---------|
| **Ubicación** | `AporteServiceTest.java` |
| **Severidad** | **Alta** |

**Por qué es un problema**

Los tests cubren camino feliz, umbral de revisión y dos casos negativos básicos. **No hay test del tope mensual** (H-01 pasaría desapercibido), ni de concurrencia, idempotencia, periodo de saldo ni del endpoint de consolidado. Los tests felices dan falsa confianza.

**Corrección propuesta**

Agregar como mínimo:
- `registrar_superaTopeMensual_lanzaExcepcion` con `AF-003`
- `registrar_llegaExactoAlTope_esValido`
- Test de integración del consolidado
- Test de idempotencia (una vez implementada)

---

## Hallazgos de severidad media

### H-13 · Excepciones de negocio sin manejo HTTP adecuado

| Campo | Detalle |
|-------|---------|
| **Ubicación** | `AporteService.java` — `IllegalArgumentException` sin `@ControllerAdvice` |
| **Severidad** | **Media** |

**Por qué es un problema**

`IllegalArgumentException` sin handler global devuelve 500 Internal Server Error. Un afiliado inexistente o monto inválido debería responder 400/404/422 con cuerpo estructurado (`ProblemDetail` RFC 7807).

**Corrección propuesta**

Crear excepciones de dominio (`AfiliadoNoEncontradoException`, `TopeMensualExcedidoException`) y un `@RestControllerAdvice` que las mapee a códigos HTTP correctos.

---

### H-14 · Consola H2 habilitada

| Campo | Detalle |
|-------|---------|
| **Ubicación** | `application.properties`, líneas 13–15 |
| **Severidad** | **Media** |

**Por qué es un problema**

`spring.h2.console.enabled=true` expone acceso directo a la base de datos vía web. Aceptable en local para la auditoría, pero un blocker en cualquier perfil que no sea `dev`.

**Corrección propuesta**

Restringir a `@Profile("dev")` o deshabilitar por defecto. En producción usar PostgreSQL con credenciales gestionadas.

---

### H-15 · Evento de auditoría desacoplado del aporte persistido

| Campo | Detalle |
|-------|---------|
| **Ubicación** | `AporteService.java:62–66`, `EventoAporte.java` |
| **Severidad** | **Media** |

**Por qué es un problema**

El evento se persiste *antes* del aporte y no guarda referencia al `aporteId`. Si se necesita reconciliar eventos con registros contables, no hay FK ni correlación. Además complica el rollback lógico.

**Corrección propuesta**

Persistir el aporte primero, luego el evento con `aporteId`. O usar transactional outbox con el ID ya asignado.

---

### H-16 · Log operacional sin trazabilidad de auditoría regulada

| Campo | Detalle |
|-------|---------|
| **Ubicación** | `AporteService.java:64` |
| **Severidad** | **Media** |

**Por qué es un problema**

Un único `log.info` con monto y afiliado no cumple requisitos de trazabilidad SFC: falta correlation ID, operador, canal, timestamp estructurado, resultado de la operación. No es inmutable ni append-only.

**Corrección propuesta**

Log estructurado (JSON) con MDC/correlation ID. Tabla de auditoría append-only o integración con sistema de eventos corporativo.

---

## Hallazgos de severidad baja

### H-17 · Umbral de revisión en el límite exacto

| Campo | Detalle |
|-------|---------|
| **Ubicación** | `AporteService.java:60` — `monto > umbralRevision` |
| **Severidad** | **Baja** |

**Por qué es un problema**

Un aporte de exactamente 5.000.000 no se marca para revisión (`>` estricto). Puede ser intencional, pero conviene confirmar con negocio si debe ser `>=`.

**Corrección propuesta**

Alinear con regla de negocio. Documentar en código o spec.

---

### H-18 · `@Data` en entidades JPA

| Campo | Detalle |
|-------|---------|
| **Ubicación** | `Aporte.java`, `Saldo.java`, `EventoAporte.java` |
| **Severidad** | **Baja** |

**Por qué es un problema**

Lombok `@Data` genera `equals`/`hashCode` incluyendo campos mutables, lo que puede causar comportamiento inesperado en entidades gestionadas por Hibernate.

**Corrección propuesta**

Usar `@Getter`/`@Setter` y definir `equals`/`hashCode` solo por ID, o migrar a records inmutables en capa de dominio separada de entidades JPA.

---

## Priorización para el revisor del MR

| Orden | ID | Severidad | ¿Bloquea MR? |
|-------|----|-----------|--------------|
| 1 | H-01 | Crítica | Sí — corrección monetaria rota |
| 2 | H-02 | Crítica | Sí — OWASP A03 Injection |
| 3 | H-03 | Crítica | Sí — integridad bajo concurrencia |
| 4 | H-04 | Crítica | Sí — duplicación de aportes |
| 5 | H-05 | Crítica | Sí — estándar CIS para dinero |
| 6 | H-07 | Crítica | Sí — sin controles de acceso |
| 7 | H-06 | Alta | Sí — riesgo de estado inconsistente |
| 8 | H-08 | Alta | Sí — acumulación en periodo incorrecto |
| 9 | H-09 | Alta | Sí — arquitectura CIS |
| 10 | H-11 | Alta | Sí — entrada no validada |
| 11 | H-12 | Alta | Sí — tests no cubren reglas críticas |
| 12 | H-10 | Alta | Recomendado antes de merge |
| 13–18 | Resto | Media/Baja | Seguimiento en iteración |

---

## Notas del proceso de auditoría

**Enfoque:** Revisión estática del código fuente como MR de célula CIS. Prioricé lo que afecta integridad financiera (tope, concurrencia, idempotencia, tipos numéricos) y seguridad (SQLi, auth) sobre observaciones estilísticas.

**Pista del brief aplicada:** El bug del tope con `==` es el más sutil — los tests felices no lo detectan porque ningún caso prueba el límite mensual. La concatenación SQL en el GET es el hallazgo de seguridad más evidente. La ausencia de `@Transactional` + orden de persistencia convierte fallos transitorios en inconsistencias contables.

**Prompt de auditoría utilizado (referencia):**
> Revisar módulo Spring Boot de aportes voluntarios como MR en contexto financiero regulado SFC. Buscar: corrección numérica, concurrencia, idempotencia, seguridad, límites Clean Architecture/CQRS/SOLID. Priorizar lo que bloquearía aprobación, no cantidad de hallazgos.
