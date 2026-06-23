# Reto A — Hallazgos de auditoría

Módulo: `reto-a` · Servicio de aportes voluntarios  
Revisión tipo MR · Contexto: sistema financiero regulado (SFC)

---

## Resumen ejecutivo

El módulo compila y los tests felices pasan, pero **no es mergeable**. Se identifican fallos críticos en integridad financiera, concurrencia, idempotencia, seguridad y arquitectura. Los hallazgos priorizan riesgo real en producción bancaria.

---

## Hallazgos bloqueantes

### H-01 · Validación de tope mensual incorrecta + uso de double
- **Ubicación:** `AporteService.java`
- **Severidad:** Crítica

Comparación incorrecta (`==`) y uso de `double` generan validación inexacta del límite mensual, permitiendo sobrepasar el tope o rechazar casos válidos.

**Riesgo:** Inconsistencia financiera y fuga de control de aportes.  
**Corrección:** Migrar a `BigDecimal` y validar con `compareTo`.

---

### H-02 · SQL Injection en consulta de consolidado
- **Ubicación:** `AporteController.java`
- **Severidad:** Crítica

Concatenación directa de parámetros en query SQL.

**Riesgo:** Manipulación de consultas (OWASP A03).  
**Corrección:** PreparedStatements o repositorio JPA.

---

### H-03 · Race condition en actualización de saldo
- **Ubicación:** `AporteService`, `Saldo`
- **Severidad:** Crítica

Read-modify-write sin control de concurrencia.

**Riesgo:** pérdida de actualizaciones y desbalance financiero.  
**Corrección:** `@Version` o actualización atómica.

---

### H-04 · Ausencia de idempotencia
- **Ubicación:** `AporteService`
- **Severidad:** Crítica

No existe control contra reintentos o duplicados.

**Riesgo:** doble cobro o doble registro.  
**Corrección:** Idempotency-Key + restricción única.

---

### H-05 · Uso de double para dinero
- **Ubicación:** entidades y service
- **Severidad:** Crítica

Representación no exacta de valores monetarios.

**Riesgo:** errores de redondeo acumulativos.  
**Corrección:** `BigDecimal(precision, scale)`.

---

### H-06 · Falta de transacción
- **Ubicación:** `AporteService.registrar()`
- **Severidad:** Alta

Múltiples escrituras sin atomicidad.

**Riesgo:** estados parciales inconsistentes.  
**Corrección:** `@Transactional`.

---

### H-07 · Sin autenticación/autorización
- **Ubicación:** global
- **Severidad:** Crítica

Endpoints expuestos sin control de acceso.

**Riesgo:** manipulación total del sistema.  
**Corrección:** Spring Security + roles.

---

## Hallazgos de alta severidad

### H-08 · Validación de periodo de saldo ausente
- Riesgo de uso de saldo de mes incorrecto.

### H-09 · Violación CQRS / Clean Architecture
- GET con SQL directo en controller.

### H-10 · Entidad JPA expuesta en API
- Acoplamiento contrato–persistencia.

### H-11 · Falta de validación de input
- Ausencia de Bean Validation.

### H-12 · Tests incompletos
- No cubren reglas críticas (tope, concurrencia, idempotencia).

---

## Hallazgos de severidad media

### H-13 · Manejo incorrecto de excepciones HTTP
- Excepciones no mapeadas a códigos de negocio.

### H-14 · H2 console habilitada
- Exposición innecesaria en runtime.

### H-15 · Evento sin correlación con aporte
- Falta de trazabilidad.

### H-16 · Logging no estructurado
- Sin correlation ID ni auditoría completa.

---

## Hallazgos de baja severidad

### H-17 · Umbral de revisión estricto (`>`)
- Posible ajuste a negocio (`>=`).

### H-18 · Uso de `@Data` en entidades JPA
- Riesgo en equals/hashCode con entidades mutables.

---

## Priorización

1. H-01 a H-07 → bloqueantes de merge (finanzas + seguridad)
2. H-08 a H-12 → calidad de dominio
3. H-13 a H-18 → hardening

---

## Nota de auditoría

Enfoque basado en integridad financiera, concurrencia e idempotencia. El sistema no es seguro ni consistente para producción bancaria en su estado actual.