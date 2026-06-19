# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> Actualizado el 19/06/2026 · Protección S.A. — Equipo de Tecnología CIS

---

## Contexto del proyecto

Prueba técnica AI-First (nivel senior) del Centro de Ingeniería de Software de Protección S.A. Contiene dos retos independientes sobre un mismo dominio: aportes al fondo de inversión voluntaria.

- **reto-a** — microservicio con **defectos deliberados**. Solo se audita; nunca se modifica. Los 12 hallazgos están documentados en `reto-a/HALLAZGOS.md` con veredicto `BLOQUEADO`.
- **reto-b** — implementación completa: backend Spring Boot (Clean Architecture + CQRS) + frontend React con shadcn/ui, sobre PostgreSQL real vía Docker Compose.

---

## Cómo ejecutar el proyecto

### Infraestructura (prerequisito)
```bash
# Desde la raíz — requiere Docker Desktop corriendo
docker compose up -d
# PostgreSQL 16 Alpine en localhost:5433 (puerto 5433 porque el sistema tiene PostgreSQL 18 local en 5432)
# DB: proteccion_reto · usuario: postgres · contraseña: postgres
```

### Backend reto-b
Ejecutar `RetoBApplication` desde IntelliJ IDEA. El Maven Wrapper (`mvnw`) existe pero en este entorno Windows se recomienda usar el Maven embebido de IntelliJ.
- Puerto: `http://localhost:8082`
- Flyway aplica `V1__init.sql` automáticamente al arrancar.

### Backend reto-a
Ejecutar `RetoAApplication` desde IntelliJ. Usa H2 en memoria; no requiere Docker.
- Puerto: `http://localhost:8080` (por defecto Spring Boot)

### Frontend reto-b
```bash
cd reto-b/frontend
npm install      # solo la primera vez
npm run dev      # http://localhost:5173
```

### Tests backend reto-b
Desde IntelliJ: ejecutar `RegistrarAporteUseCaseImplTest` (unitarios, Mockito) y `AporteControllerIT` (integración, MockMvc + H2 en memoria). Los tests de integración sobreescriben el datasource a H2 via `@TestPropertySource`; no requieren Docker.

---

## Arquitectura — reto-a

Estructura por capa técnica plana (`controller` / `service` / `domain` / `dto` / `repository`). Cada defecto existe de forma deliberada para el ejercicio de code review:

| Defecto clave | Ubicación |
|---|---|
| Inyección SQL directa con `JdbcTemplate` | `AporteController.java` — endpoint consolidado |
| Dinero con `double` (pérdida de precisión) | `Aporte`, `Saldo`, `EventoAporte`, `AporteRequest` |
| Tope mensual con `==` en lugar de `>` | `AporteService.java` |
| Sin `@Transactional` ni bloqueo — race condition | `AporteService.registrar()` |
| Entidades JPA (`@Entity`) en el paquete `domain` | Acoplamiento infraestructura/dominio |
| EventoAporte guardado antes que el Aporte (FK null) | `AporteService.java` |
| PII del afiliado en logs planos | `AporteService.java` |
| Sin Spring Security ni idempotencia | Todo el módulo |

Ver análisis completo en `reto-a/HALLAZGOS.md`.

---

## Arquitectura — reto-b/backend

Clean Architecture hexagonal. Las dependencias apuntan siempre hacia el dominio:

```
infrastructure  →  application  →  domain
(JPA, REST, config)  (casos de uso)  (modelos, puertos, excepciones)
```

### Capas y responsabilidades

**`domain/`** — sin dependencias de framework
- `model/`: `Aporte`, `SaldoMensual`, `ConsolidadoAportes` — objetos de negocio puros con `BigDecimal` para montos
- `port/in/`: `RegistrarAporteUseCase` (Command + record `RegistrarAporteCommand`), `ConsultarAportesUseCase` (Query + record `ConsultarAportesQuery`)
- `port/out/`: `AporteRepositoryPort`, `SaldoRepositoryPort` — interfaces que define el dominio, implementadas en infraestructura
- `exception/`: `ReglaNegocioException(codigo, mensaje)` — única excepción de negocio tipada

**`application/`** — orquestación de casos de uso
- `RegistrarAporteUseCaseImpl` — `@Transactional`: idempotencia → validar monto → calcular periodo → buscar/inicializar saldo → validar tope → marcar revisión → persistir aporte → actualizar saldo
- `ConsultarAportesUseCaseImpl` — `@Transactional(readOnly=true)`: valida `periodoDesde ≤ periodoHasta`, delega al repositorio, acumula total

**`infrastructure/persistence/`**
- `entity/`: `AporteEntity`, `SaldoMensualEntity` (con `@Version` para optimistic locking)
- `repository/`: `SpringDataAporteRepository`, `SpringDataSaldoRepository` (Spring Data JPA)
- `adapter/`: `JpaAporteRepositoryAdapter`, `JpaSaldoRepositoryAdapter` — mapeo bidireccional Entity ↔ Domain model

**`infrastructure/web/`**
- `AporteController` — dos endpoints, lee `Idempotency-Key` del header HTTP
- `GlobalExceptionHandler` (`@RestControllerAdvice`) — mapeo de excepciones a HTTP:
  - `ReglaNegocioException` → `422` con `{timestamp, codigo, mensaje}`
  - `MethodArgumentNotValidException` → `400` con código `VALIDACION_FALLIDA`
  - `MissingRequestHeaderException` → `400` con código `HEADER_REQUERIDO_AUSENTE`
  - `OptimisticLockException` → `409` con código `CONFLICTO_CONCURRENCIA`
  - `Exception` genérica → `500`
- `CorsConfig` — permite `http://localhost:5173`

### Endpoints

| Método | Ruta | Header requerido | Descripción |
|--------|------|-----------------|-------------|
| `POST` | `/api/aportes` | `Idempotency-Key: <uuid>` | Registra un aporte; retorna `201` con `AporteResponse` |
| `GET` | `/api/aportes/{afiliadoId}/consolidado` | — | Consolida aportes por rango `periodoDesde`/`periodoHasta` (`YYYY-MM`) |

### Reglas de negocio configurables (`application.properties`)
```properties
aporte.tope-mensual=10000000      # tope acumulado por afiliado/mes en COP
aporte.umbral-revision=5000000    # montos mayores se marcan marcadaRevision=true
```

### Persistencia
- Flyway gestiona el schema: `src/main/resources/db/migration/V1__init.sql`
- Tablas: `aporte` (con `idempotencia_key UNIQUE`), `saldo_mensual` (con `version` para optimistic lock), `evento_aporte`
- Todos los montos en `NUMERIC(15,2)`; mapeo Java con `BigDecimal`

---

## Arquitectura — reto-b/frontend

React 18 + Vite 6, JavaScript (sin TypeScript), sin router ni estado global.

### UI
- **shadcn/ui** sobre Tailwind CSS v4 (`@tailwindcss/vite`) y Radix UI
- Componentes base en `src/components/ui/` (button, card, input, label, select, badge, alert, table, tabs, separator)
- Utilidad `cn()` en `src/lib/utils.js` (`clsx` + `tailwind-merge`)
- Tema y variables CSS en `src/index.css` (bloque `@theme`)

### Comunicación con el backend
`src/api/aportesApi.js` — cliente `fetch` nativo con función `parsearRespuesta()` que extrae `data.mensaje` y `data.codigo` del cuerpo de error y los adjunta al objeto `Error` lanzado.

El componente consume:
- `err.message` → texto descriptivo del error (viene del backend)
- `err.codigo` → mapeo a título de alerta en `TITULOS_ERROR` (`RegistrarAporte.jsx`)

### Flujo de idempotencia en el frontend
`useRef(crypto.randomUUID())` genera la clave al montar el componente. Se rota a un nuevo UUID solo tras un submit exitoso, lo que permite reintentar sin duplicar mientras el request no haya llegado.

### Validaciones cliente (antes de llamar al API)
- `RegistrarAporte`: afiliadoId no vacío, monto > 0, fecha presente
- `ConsolidadoAportes`: afiliadoId no vacío, periodos presentes, `periodoDesde ≤ periodoHasta`

---

## Convenciones del repositorio

- **Montos**: siempre `BigDecimal`; nunca `double` ni `float`. En DB: `NUMERIC(15,2)`.
- **Periodos**: `String` en formato `YYYY-MM` — el orden lexicográfico coincide con el cronológico.
- **Datos de prueba**: IDs sintéticos `AF-00x`, montos redondos. Nunca PII real en tests ni código.
- **Commits**: rama `candidato/santiago-roldan`; mensajes en `feat(reto-x): descripción`.
- **Puerto Docker**: el sistema tiene PostgreSQL 18 local en `5432`; el contenedor de desarrollo usa `5433`.
