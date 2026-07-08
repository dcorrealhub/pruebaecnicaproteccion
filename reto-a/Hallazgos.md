# Revisión de código – Reto A

## Resumen

Se realizó una revisión del módulo de registro de aportes voluntarios bajo el enfoque de una revisión de Merge Request.

La auditoría se centró en identificar problemas que afecten la seguridad, la integridad de la información, las reglas de negocio y la arquitectura de la solución, priorizando aquellos que impedirían aprobar el Merge Request.

---

# Resumen de hallazgos

| # | Hallazgo | Ubicación | Severidad |
|---|----------|-----------|-----------|
| 1 | Uso de `double` para representar dinero | Domain / DTO / Service | 🔴 Crítica |
| 2 | Posible SQL Injection | AporteController | 🔴 Crítica |
| 3 | Validación incorrecta del tope mensual | AporteService | 🔴 Crítica |
| 4 | Operación no transaccional | AporteService | 🔴 Crítica |
| 5 | Condición de carrera en actualización del saldo | AporteService | 🟠 Alta |
| 6 | Ausencia de idempotencia | Controller / Service | 🟠 Alta |
| 7 | Violación de Clean Architecture | AporteController | 🟠 Alta |
| 8 | Validaciones HTTP insuficientes | DTO / Controller | 🟡 Media |
| 9 | Cobertura de pruebas insuficiente | Test | 🟡 Media |

---

# Hallazgo 1

## Uso de `double` para representar dinero

**Ubicación**

- Aporte
- Saldo
- EventoAporte
- AporteRequest
- AporteService

**Severidad**

🔴 Crítica

### Problema

Los montos monetarios se almacenan utilizando `double`.

Este tipo de dato utiliza representación binaria de punto flotante, por lo que no garantiza precisión decimal. En un sistema financiero esto puede producir errores acumulativos en los cálculos.

### Impacto

- Descuadres contables.
- Errores de redondeo.
- Inconsistencias en los saldos.

### Corrección propuesta

Reemplazar `double` por `BigDecimal` y realizar comparaciones mediante `compareTo()`.

---

# Hallazgo 2

## Posible SQL Injection

**Ubicación**

AporteController

**Severidad**

🔴 Crítica

### Problema

La consulta SQL concatena directamente parámetros provenientes del cliente.

Esto permite alterar la consulta mediante entradas maliciosas.

### Impacto

- Exposición de información.
- Riesgo de acceso no autorizado.

### Corrección propuesta

Utilizar consultas parametrizadas o delegar la consulta al repositorio de Spring Data JPA.

---

# Hallazgo 3

## Validación incorrecta del tope mensual

**Ubicación**

AporteService

**Severidad**

🔴 Crítica

### Problema

La validación utiliza igualdad (`==`) para determinar si el aporte supera el tope.

Con esta implementación únicamente se rechaza el caso donde el acumulado sea exactamente igual al límite, permitiendo montos superiores.

### Corrección propuesta

Comparar utilizando `>` o `BigDecimal.compareTo()` según el tipo utilizado.

---

# Hallazgo 4

## Operación no transaccional

**Ubicación**

AporteService

**Severidad**

🔴 Crítica

### Problema

Durante el registro del aporte se realizan varias escrituras independientes.

Si una de ellas falla, el sistema puede quedar en un estado inconsistente.

### Corrección propuesta

Encapsular toda la operación dentro de una transacción mediante `@Transactional`.

---

# Hallazgo 5

## Condición de carrera

**Ubicación**

AporteService

**Severidad**

🟠 Alta

### Problema

Dos solicitudes concurrentes pueden leer el mismo saldo antes de actualizarlo.

Esto puede provocar pérdida de actualizaciones y permitir superar el tope mensual.

### Corrección propuesta

Implementar control de concurrencia mediante bloqueo optimista (`@Version`) o pesimista.

---

# Hallazgo 6

## Ausencia de idempotencia

**Ubicación**

Controller / Service

**Severidad**

🟠 Alta

### Problema

Si un cliente reintenta la misma solicitud, el sistema registra nuevamente el aporte.

### Corrección propuesta

Implementar una clave de idempotencia (`Idempotency-Key`) para identificar solicitudes repetidas.

---

# Hallazgo 7

## Violación de Clean Architecture

**Ubicación**

AporteController

**Severidad**

🟠 Alta

### Problema

El controlador construye consultas SQL directamente.

La capa de presentación debería delegar esta responsabilidad a un servicio o repositorio.

### Corrección propuesta

Mover la lógica de acceso a datos hacia la capa correspondiente.

---

# Hallazgo 8

## Validaciones HTTP insuficientes

**Ubicación**

DTO / Controller

**Severidad**

🟡 Media

### Problema

No se utilizan anotaciones de Bean Validation (`@Valid`, `@NotNull`, `@Positive`, etc.).

Además, las excepciones de negocio terminan devolviendo errores HTTP 500.

### Corrección propuesta

Implementar Bean Validation y un `@RestControllerAdvice` para mapear correctamente los errores.

---

# Hallazgo 9

## Cobertura de pruebas insuficiente

**Ubicación**

AporteServiceTest

**Severidad**

🟡 Media

### Problema

Las pruebas actuales cubren únicamente escenarios exitosos y algunas validaciones básicas.

No existen pruebas para:

- límite mensual;
- concurrencia;
- idempotencia;
- transacciones;
- reinicio del período mensual.

### Corrección propuesta

Agregar pruebas que cubran las reglas de negocio críticas y los casos límite.

---

# Conclusión

La revisión permitió identificar varios aspectos que deberían corregirsew antes de considerar este módulo listo para un entorno real. Los hallazgos encontrados incluyen problemas que pueden afectar el funcionamiento esperado, la seguridad de la información y la confiabilidad del sistema.

Además de señalar los inconvenientes, se propusieron alternativas de solución para cada uno de ellos, priorizando aquellos con mayor impacto sobre el negocio y la experiencia del usuario.

En general, la base del proyecto es clara y entendible, pero es recomendable corregir primero los hallazgos de mayor severidad antes de continuar con nuevas funcionalidades o llevar la aplicación a producción.