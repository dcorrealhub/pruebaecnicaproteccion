# Plan de Desarrollo TDD — Reto B

## Stack del proyecto
- **Backend:** Java 21, Spring Boot 3.4, Clean Architecture, PostgreSQL, Flyway
- **Frontend:** React 18 + Vite, fetch nativo
- **BD:** PostgreSQL via Docker

---

## Ciclo TDD para cada etapa: **Red → Green → Refactor**

---

## Fase 1: Infraestructura base

| Paso | Descripción | Agente |
|------|-------------|--------|
| 1.1 | Levantar Docker Compose (PostgreSQL) y verificar conexión | — |
| 1.2 | Escribir test de integración para `SpringDataAporteRepository` (findByIdempotenciaKey, findByAfiliadoIdAndPeriodoBetween) | `@testing-expert` |
| 1.3 | Escribir test de integración para `SpringDataSaldoRepository` (findByAfiliadoIdAndMes, guardar con versión optimista) | `@testing-expert` |
| 1.4 | Implementar `JpaAporteRepositoryAdapter` (mapeo Entity ↔ Domain) | `@java-backend-expert` |
| 1.5 | Implementar `JpaSaldoRepositoryAdapter` (mapeo Entity ↔ Domain, manejo de OptimisticLockException) | `@java-backend-expert` |

---

## Fase 2: Caso de uso — Consultar consolidado

| Paso | Descripción | Agente |
|------|-------------|--------|
| 2.1 | **RED:** Test unitario para `ConsultarAportesUseCaseImpl.consultar()` — mock de `AporteRepositoryPort`, validar total y detalle | `@testing-expert` |
| 2.2 | **GREEN:** Implementar `ConsultarAportesUseCaseImpl` — buscar aportes, sumar montos con `BigDecimal.add`, retornar `ConsolidadoAportes` | `@java-backend-expert` |
| 2.3 | **REFACTOR:** Validar edge cases (sin resultados, periodos invertidos) | `@java-backend-expert` |
| 2.4 | **RED:** Test de integración para endpoint `GET /api/aportes/consolidado` (con Testcontainers o H2) | `@testing-expert` |
| 2.5 | **GREEN:** Implementar/validar controller existente — `ConsolidadoResponse.from()` ya está listo | `@java-backend-expert` |

---

## Fase 3: Caso de uso — Registrar aporte

| Paso | Descripción | Agente |
|------|-------------|--------|
| 3.1 | **RED:** Test unitario — idempotencia: si ya existe aporte con misma `idempotenciaKey`, retornar el existente sin duplicar | `@testing-expert` |
| 3.2 | **RED:** Test unitario — monto negativo/cero lanza `IllegalArgumentException` | `@testing-expert` |
| 3.3 | **RED:** Test unitario — monto + saldo actual > topeMensual lanza excepción | `@testing-expert` |
| 3.4 | **RED:** Test unitario — monto > umbralRevision marca `marcadaRevision = true` | `@testing-expert` |
| 3.5 | **RED:** Test unitario — flujo feliz: saldo se actualiza, aporte se guarda, evento se registra | `@testing-expert` |
| 3.6 | **GREEN:** Implementar `RegistrarAporteUseCaseImpl` — toda la lógica de negocio | `@java-backend-expert` |
| 3.7 | **REFACTOR:** Extraer validaciones a métodos privados, asegurar `@Transactional` | `@java-backend-expert` |
| 3.8 | **RED:** Test de integración — `POST /api/aportes` con datos válidos responde `201 CREATED` | `@testing-expert` |
| 3.9 | **RED:** Test de integración — mismo `idempotenciaKey` dos veces responde `200` con mismo ID (idempotencia) | `@testing-expert` |
| 3.10 | **GREEN:** Asegurar que controller + use case + adapters responden correctamente | `@java-backend-expert` |

---

## Fase 4: Frontend — RegistrarAporte

| Paso | Descripción | Agente |
|------|-------------|--------|
| 4.1 | **RED:** Test unitario — `registrarAporte()` en `aportesApi.js` hace fetch a `POST /api/aportes` con body correcto | `@testing-expert` |
| 4.2 | **GREEN:** Implementar `registrarAporte()` en `aportesApi.js` con fetch nativo | `@frontend-expert` |
| 4.3 | **RED:** Test de componente — formulario `RegistrarAporte` valida monto > 0 antes de enviar | `@testing-expert` |
| 4.4 | **RED:** Test de componente — muestra mensaje de éxito al registrar | `@testing-expert` |
| 4.5 | **RED:** Test de componente — muestra alerta si el aporte queda marcado para revisión | `@testing-expert` |
| 4.6 | **GREEN:** Completar `RegistrarAporte.jsx` (conectar formulario con API, manejar estados) | `@frontend-expert` |

---

## Fase 5: Frontend — ConsolidadoAportes

| Paso | Descripción | Agente |
|------|-------------|--------|
| 5.1 | **RED:** Test unitario — `consultarConsolidado()` en `aportesApi.js` hace fetch a `GET /api/aportes/consolidado` con query params | `@testing-expert` |
| 5.2 | **GREEN:** Implementar `consultarConsolidado()` en `aportesApi.js` | `@frontend-expert` |
| 5.3 | **RED:** Test de componente — `ConsolidadoAportes` muestra tabla con resultados | `@testing-expert` |
| 5.4 | **RED:** Test de componente — muestra "No se encontraron aportes" si no hay datos | `@testing-expert` |
| 5.5 | **GREEN:** Completar `ConsolidadoAportes.jsx` (conectar con API, mostrar tabla/total) | `@frontend-expert` |

---

## Fase 6: Integración final y validación

| Paso | Descripción | Agente |
|------|-------------|--------|
| 6.1 | Test end-to-end: Docker compose up → registrar aporte → consultar consolidado → verificar datos | `@testing-expert` |
| 6.2 | Prueba de idempotencia: mismo request dos veces, mismo resultado | `@testing-expert` |
| 6.3 | Prueba de concurrencia: dos requests simultáneos, sin lost update | `@java-backend-expert` |
| 6.4 | Prueba de validación: datos inválidos → 400 Bad Request con mensajes | `@testing-expert` |

---

## Resumen de agentes por fase

| Agente | Fases | Rol |
|--------|-------|-----|
| `@testing-expert` | 1, 2, 3, 4, 5, 6 | Escribe los tests (RED) y valida |
| `@java-backend-expert` | 1, 2, 3, 6 | Implementa backend (GREEN) |
| `@frontend-expert` | 4, 5 | Implementa frontend (GREEN) |

**Flujo:** Por cada ficha (ej: 3.1) iniciar con `@testing-expert` para el test (RED), luego el agente de implementación para pasar el test (GREEN).
