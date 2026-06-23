# Diseño — Módulo de Aportes Voluntarios

**Proyecto:** reto-b · **Stack:** Java 21, Spring Boot 3.4, PostgreSQL, Flyway  
**Alcance:** registro idempotente y consulta consolidada de aportes por afiliado y periodo.

---

## Contexto

Módulo backend para fondos de pensiones voluntarios. Debe soportar alta concurrencia en registro, trazabilidad mínima y reglas financieras configurables sin acoplar el núcleo a Spring ni JPA.

---

## Arquitectura

**Clean Architecture / Hexagonal** con cuatro capas y dependencias unidireccionales:

```
api  →  application  →  domain  ←  infrastructure
```

| Capa | Responsabilidad |
|------|-----------------|
| `domain` | Modelos, reglas de negocio, puertos (interfaces). Sin frameworks. |
| `application` | Casos de uso, orquestación transaccional, configuración de negocio. |
| `infrastructure` | JPA, adapters de persistencia, Flyway. |
| `api` | Controllers REST, DTOs, mapeo HTTP, manejo de errores. |

**Principio:** los controllers no contienen lógica de negocio. Validación sintáctica en DTOs (`@Valid`); reglas de dominio en `AporteDomainService` y casos de uso.

---

## Estructura de paquetes

```
co.proteccion.cis.retob
├── domain
│   ├── model/          Aporte, SaldoMensual, ConsolidadoAportes, EstadoAporte
│   ├── service/        AporteDomainService
│   ├── exception/      excepciones de dominio tipadas
│   └── port/
│       ├── in/         RegistrarAporteUseCase, ConsultarAportesUseCase
│       └── out/        AporteRepositoryPort, SaldoRepositoryPort,
│                         EventoAporteRepositoryPort, ClockPort
├── application
│   ├── usecase/        *UseCaseImpl
│   └── config/         AporteProperties
├── infrastructure
│   └── persistence/    entity, repository, adapter, mapper
└── api
    ├── controller/     AporteController
    ├── dto/            Request / Response
    ├── mapper/         AporteApiMapper
    └── exception/      GlobalExceptionHandler
```

---

## Modelo de dominio

| Entidad | Descripción |
|---------|-------------|
| `Aporte` | Aporte individual. Monto en `BigDecimal`. Periodo `YYYY-MM`. Estado: `REGISTRADO` \| `REQUIERE_REVISION`. Clave de idempotencia obligatoria. |
| `SaldoMensual` | Acumulado mensual por afiliado. Control de concurrencia optimista (`version`). |
| `ConsolidadoAportes` | Resultado de consulta: total + detalle de aportes en rango de periodos. |
| `EventoAporte` | Auditoría desacoplada (`APORTE_REGISTRADO`, `APORTE_REVERSADO`). |

`AporteDomainService` concentra reglas puras: monto positivo, validación de tope, determinación de estado y cálculo de periodo.

---

## Reglas de negocio

| Regla | Implementación |
|-------|----------------|
| Monto > 0 | `AporteDomainService.validarMontoPositivo` |
| Idempotencia obligatoria | `UNIQUE(idempotencia_key)` en BD + consulta previa en caso de uso |
| Tope mensual por afiliado | Configurable (`aporte.tope-mensual`). Validado contra `SaldoMensual` del periodo |
| Umbral de revisión | Si `monto > umbral` → estado `REQUIERE_REVISION` (config: `aporte.umbral-revision`) |
| Consolidado | Por `afiliadoId` + rango `periodoDesde`–`periodoHasta` |

Montos siempre en `BigDecimal`. Nunca tipos primitivos de punto flotante.

---

## Casos de uso

### Registrar aporte (`RegistrarAporteUseCase`)

1. Buscar por `idempotenciaKey` → si existe, retornar aporte existente.
2. Validar monto y calcular periodo del día.
3. Obtener/inicializar `SaldoMensual` del periodo.
4. Validar que `saldo + monto ≤ topeMensual`.
5. Determinar estado según umbral de revisión.
6. Persistir aporte, actualizar saldo (retry ante conflicto de versión).
7. Registrar evento de auditoría.

Transaccional. Concurrencia en saldo vía optimistic locking.

### Consultar consolidado (`ConsultarAportesUseCase`)

Solo lectura. Lista aportes del afiliado en el rango de periodos y calcula `totalAportado`.

---

## API REST

| Método | Ruta | Descripción |
|--------|------|-------------|
| `POST` | `/api/aportes` | Registra aporte. Header/body incluye `idempotenciaKey`. |
| `GET` | `/api/aportes/consolidado` | Query: `afiliadoId`, `periodoDesde`, `periodoHasta`. |

**DTOs:** `RegistrarAporteRequest`, `AporteResponse`, `ConsolidadoResponse`, `ErrorResponse`.

**Errores esperados:**

| Condición | HTTP |
|-----------|------|
| Monto inválido | 400 |
| Tope mensual excedido | 422 |
| Conflicto de concurrencia en saldo | 409 |
| Reintento idempotente (misma key) | 201 con aporte existente |

---

## Persistencia (PostgreSQL)

Esquema gestionado por Flyway (`V1__init.sql`):

| Tabla | Propósito |
|-------|-----------|
| `aporte` | Registro de aportes. `idempotencia_key` UNIQUE. Índice `(afiliado_id, periodo)`. |
| `saldo_mensual` | Acumulado mensual. UNIQUE `(afiliado_id, mes)`. Campo `version` para locking optimista. |
| `evento_aporte` | Trazabilidad de eventos por aporte. |

Entidades JPA en `infrastructure.persistence.entity`. Mapeo dominio ↔ entidad en adapters, no en el núcleo.

---

## Configuración

```properties
aporte.tope-mensual=10000000
aporte.umbral-revision=5000000
```

Externalizables por ambiente. Inyectadas vía `AporteProperties` en la capa application.

---

## Decisiones de diseño

1. **Dominio puro:** facilita pruebas unitarias de reglas sin contexto Spring.
2. **Puertos hexagonales:** persistencia intercambiable; el dominio no conoce JPA.
3. **Idempotencia en BD + aplicación:** garantía ante reintentos de red y condiciones de carrera.
4. **Saldo mensual como agregado:** evita recalcular totales en cada registro; soporta validación de tope eficiente.
5. **Estado enum vs boolean:** extensible a flujos de revisión manual futuros.
6. **Controllers delgados:** evaluable en code review; separación clara de responsabilidades.

---

## Fuera de alcance (v1)

- Reversión de aportes (tabla `evento_aporte` preparada).
- Notificaciones / integración con sistemas externos.
- Autenticación y autorización.
