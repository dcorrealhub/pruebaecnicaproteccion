# NOTAS_PROCESO — Reto B: Aportes Voluntarios

Documento de entrega técnica para handoff a ingeniería.

**Stack:** Java 21 · Spring Boot 3.4 · PostgreSQL 15 · React 19 · Vite 6  
**Repositorio:** `reto-b/`

---

## 1. Resumen de implementación

### Backend

API REST para registro y consulta de aportes voluntarios a fondos de pensiones.

| Componente | Estado |
|------------|--------|
| `POST /api/aportes` — registro idempotente | Completo |
| `GET /api/aportes/consolidado` — total + detalle por afiliado y periodo | Completo |
| Persistencia PostgreSQL + Flyway (V1–V3) | Completo |
| Reglas de negocio (monto, tope, umbral, canal) | Completo |
| Idempotencia con tabla dedicada + claim atómico | Completo |
| Concurrencia optimista en saldo mensual | Completo |
| Auditoría mínima (`evento_aporte`) | Completo |
| Tests unitarios (dominio + casos de uso) | Completo |

**No implementado (fuera de alcance v1):** autenticación/autorización, reversión de aportes, notificaciones, CORS explícito en backend.

### Frontend

SPA mínima en React + Vite para operación manual del API.

| Componente | Estado |
|------------|--------|
| Formulario de registro de aporte | Completo |
| Consulta de consolidado por afiliado y periodo | Completo |
| Tabla de resultados + resumen (total, cantidad) | Completo |
| `fetch` nativo, estado con `useState` | Completo |
| Proxy de desarrollo hacia backend | Completo |

### Funcionalidades completas end-to-end

1. Registrar aporte con idempotencia (reintentos no duplican).
2. Validar tope mensual y marcar revisión por umbral.
3. Consultar consolidado: total agregado + lista de aportes en rango de periodos.
4. UI funcional contra el backend local.

---

## 2. Arquitectura implementada

### Estructura backend

Arquitectura hexagonal (Clean Architecture) con dependencias hacia el dominio.

```
co.proteccion.cis.retob
├── domain/           Modelos, reglas puras, puertos (interfaces), excepciones
├── application/      Casos de uso, IdempotenciaAporteService, configuración
├── infrastructure/
│   ├── web/          Controllers REST, DTOs, GlobalExceptionHandler
│   ├── persistence/  Entidades JPA, repositories Spring Data, adapters
│   └── clock/        Adaptador de fecha (ClockPort)
```

| Capa | Responsabilidad | Ejemplos |
|------|-----------------|----------|
| **Controller** (`infrastructure/web`) | HTTP, validación sintáctica (`@Valid`), mapeo DTO ↔ comando | `AporteController` |
| **Service / Use case** (`application/usecase`) | Orquestación transaccional, invoca dominio y puertos | `RegistrarAporteUseCaseImpl`, `ConsultarAportesUseCaseImpl` |
| **Repository** (`infrastructure/persistence`) | Implementación de puertos de salida vía JPA | `JpaAporteRepositoryAdapter`, `JpaIdempotenciaRepositoryAdapter` |
| **Domain** (`domain/`) | Reglas de negocio sin frameworks | `AporteDomainService`, modelos `Aporte`, `SaldoMensual` |

Los controllers no contienen lógica de negocio. Los casos de uso dependen de **puertos** (`AporteRepositoryPort`, `IdempotenciaRepositoryPort`, etc.), no de JPA directamente.

### Decisiones clave

| Decisión | Motivo |
|----------|--------|
| **BigDecimal** para montos | Precisión decimal en operaciones financieras; `NUMERIC(15,2)` en BD |
| **Idempotencia en tabla dedicada** (`idempotencia_aporte`) + `UNIQUE` en `aporte.idempotencia_key` | Claim atómico ante concurrencia; defensa en profundidad |
| **DTOs en API** (`RegistrarAporteRequest`, `AporteResponse`, `ConsolidadoResponse`) | No se exponen entidades JPA ni modelos de dominio |
| **Estado enum** (`REGISTRADO`, `REQUIERE_REVISION`) | Reemplaza boolean `marcada_revision`; extensible a flujos de revisión |
| **Saldo mensual agregado** | Validación de tope O(1) sin recalcular sumas en cada registro |
| **Optimistic locking** (`version` en `saldo_mensual`) | Reintentos ante conflictos de concurrencia en saldo |
| **Flyway** | Esquema versionado; `ddl-auto=validate` |

### Separación de responsabilidades

- **Validación HTTP:** Jakarta Validation en DTOs + `@ModelAttribute` / `@RequestBody`.
- **Reglas de negocio:** `AporteDomainService` (monto, tope, periodo, canal, umbral).
- **Persistencia:** Adapters traducen dominio ↔ entidad JPA (`AportePersistenceMapper`).
- **Idempotencia:** `IdempotenciaAporteService` coordina claim/completar/liberar; el caso de uso de registro solo orquesta.

---

## 3. Reglas de negocio implementadas

### Validación de monto > 0

- `AporteDomainService.validarMontoPositivo(monto)` — rechaza `null`, cero o negativos.
- Constraint BD: `CHECK (monto > 0)` en tabla `aporte`.
- Error: `400` — código `MONTO_INVALIDO`.

### Tope mensual por afiliado

- Configurable: `aporte.tope-mensual` (default `10000000`).
- Se valida contra `SaldoMensual` del periodo (`YYYY-MM`) del aporte.
- Si `saldo_actual + monto > tope` → excepción.
- Error: `422` — código `TOPE_MENSUAL_EXCEDIDO`.
- Tras registro exitoso se actualiza `saldo_mensual.total`.

### Idempotencia (mecanismo técnico)

Flujo en `RegistrarAporteUseCaseImpl`:

1. **Consulta rápida:** si existe registro `COMPLETADO` en `idempotencia_aporte` → devuelve el aporte original.
2. **Claim:** `INSERT` en `idempotencia_aporte` con estado `EN_PROCESO` y `UNIQUE(idempotencia_key)`. El primer hilo gana; los demás reciben violación de unicidad.
3. **Procesamiento:** validaciones, persistencia de aporte, actualización de saldo, evento de auditoría.
4. **Completar:** actualiza registro a `COMPLETADO` con `aporte_id`.
5. **Fallo:** elimina el claim `EN_PROCESO` para permitir reintento con la misma clave.
6. **Concurrencia perdedora:** lee registro existente; si `COMPLETADO` devuelve aporte; si `EN_PROCESO` → `409 IDEMPOTENCIA_EN_PROCESO`.

Clave obligatoria vía header `Idempotency-Key` o campo body `idempotenciaKey` (prioridad: header).

Reintento con misma clave → `201` con el mismo payload (no duplica saldo ni evento).

### Marcado de revisión por umbral

- Configurable: `aporte.umbral-revision` (default `5000000`).
- Si `monto > umbral` → estado `REQUIERE_REVISION`; si no → `REGISTRADO`.
- El aporte se persiste en ambos casos; la revisión es un marcador operativo, no bloquea el registro.

---

## 4. Cómo ejecutar el proyecto

### Base de datos (PostgreSQL en Docker)

Desde la **raíz del repositorio** (`pruebaecnicaproteccion/`):

```bash
docker compose up -d
```

| Parámetro | Valor |
|-----------|-------|
| Host | `localhost` |
| Puerto | `5432` |
| Base de datos | `proteccion_reto` |
| Usuario | `postgres` |
| Contraseña | `postgres` |
| Contenedor | `proteccion_reto_db` |

Esquema gestionado por Flyway al arrancar el backend. Migraciones en `backend/src/main/resources/db/migration/`:

| Versión | Contenido |
|---------|-----------|
| V1 | Tablas `saldo_mensual`, `aporte`, `evento_aporte` + índices |
| V2 | Columna `estado` en `aporte`, checks de integridad |
| V3 | Tabla `idempotencia_aporte` + migración de claves existentes |

### Backend

Requisito: **Java 21**.

```bash
cd reto-b/backend
mvn spring-boot:run
```

Alternativa:

```bash
mvn clean package -DskipTests
java -jar target/reto-b-1.0.0.jar
```

| Parámetro | Valor |
|-----------|-------|
| Puerto | **8082** |
| Base URL API | `http://localhost:8082/api` |

Configuración relevante (`application.properties`):

```properties
server.port=8082
spring.datasource.url=jdbc:postgresql://localhost:5432/proteccion_reto
spring.datasource.username=postgres
spring.datasource.password=postgres
aporte.tope-mensual=10000000
aporte.umbral-revision=5000000
```

Tests:

```bash
mvn test
```

### Frontend

Requisito: **Node.js 18+**.

```bash
cd reto-b/frontend
npm install
npm run dev
```

| Parámetro | Valor |
|-----------|-------|
| Puerto dev | **5173** |
| URL | **http://localhost:5173** |
| Proxy | `/api` → `http://localhost:8082` (configurado en `vite.config.js`) |

Build producción:

```bash
npm run build
npm run preview   # preview en http://localhost:4173
```

Variable opcional: `VITE_API_URL=http://localhost:8082/api` (si no se usa el proxy de Vite).

**Orden de arranque recomendado:** Docker (PostgreSQL) → Backend → Frontend.

---

## 5. Contrato API

Base: `http://localhost:8082/api`

Errores estándar (`ErrorResponse`):

```json
{
  "codigo": "CODIGO_ERROR",
  "mensaje": "Descripción",
  "timestamp": "2026-06-23T12:00:00-05:00"
}
```

### POST `/aportes`

Registra un aporte voluntario. Operación idempotente.

**Headers**

| Header | Obligatorio | Descripción |
|--------|-------------|-------------|
| `Content-Type` | Sí | `application/json` |
| `Idempotency-Key` | Sí* | Clave única del cliente. Prioridad sobre body. |

\* Obligatorio en header **o** en body como `idempotenciaKey`.

**Request body**

```json
{
  "afiliadoId": "AF-001",
  "monto": 500000,
  "canal": "WEB",
  "idempotenciaKey": "opcional-si-usa-header"
}
```

| Campo | Tipo | Obligatorio | Notas |
|-------|------|-------------|-------|
| `afiliadoId` | string | Sí | |
| `monto` | number | Sí | > 0 |
| `canal` | string | Sí | `WEB`, `MOVIL`, `OFICINA`, `CALL_CENTER`, `ALIADO` |
| `idempotenciaKey` | string | Condicional | Si no se envía header |

**Response `201 Created`**

```json
{
  "id": 1,
  "afiliadoId": "AF-001",
  "monto": 500000,
  "fecha": "2026-06-23",
  "canal": "WEB",
  "periodo": "2026-06",
  "estado": "REGISTRADO"
}
```

**Errores frecuentes**

| Código HTTP | Código error | Condición |
|-------------|--------------|-----------|
| 400 | `MONTO_INVALIDO` | Monto ≤ 0 |
| 400 | `CANAL_INVALIDO` | Canal no reconocido |
| 400 | `VALIDACION` | Campos obligatorios / formato |
| 422 | `TOPE_MENSUAL_EXCEDIDO` | Supera tope mensual |
| 409 | `CONCURRENCIA_SALDO` | Conflicto optimista en saldo (agotados reintentos) |
| 409 | `IDEMPOTENCIA_EN_PROCESO` | Misma clave en procesamiento concurrente |

Reintento idempotente (misma clave, ya completada): `201` con el aporte original.

---

### GET `/aportes/consolidado`

Consolidado de aportes por afiliado en un rango de periodos.

**Query params**

| Parámetro | Obligatorio | Formato | Descripción |
|-----------|-------------|---------|-------------|
| `afiliadoId` | Sí | string | Identificador del afiliado |
| `periodoDesde` | Sí | `YYYY-MM` | Periodo inicial (inclusive) |
| `periodoHasta` | Sí | `YYYY-MM` | Periodo final (inclusive) |

**Ejemplo**

```http
GET /api/aportes/consolidado?afiliadoId=AF-001&periodoDesde=2026-01&periodoHasta=2026-06
```

**Response `200 OK`**

```json
{
  "afiliadoId": "AF-001",
  "periodo": {
    "desde": "2026-01",
    "hasta": "2026-06"
  },
  "resumen": {
    "totalAportado": 3500000,
    "cantidadAportes": 3
  },
  "aportes": [
    {
      "id": 1,
      "afiliadoId": "AF-001",
      "monto": 1000000,
      "fecha": "2026-03-15",
      "canal": "WEB",
      "periodo": "2026-03",
      "estado": "REGISTRADO"
    },
    {
      "id": 2,
      "afiliadoId": "AF-001",
      "monto": 2500000,
      "fecha": "2026-05-10",
      "canal": "MOVIL",
      "periodo": "2026-05",
      "estado": "REQUIERE_REVISION"
    }
  ]
}
```

**Errores frecuentes**

| Código HTTP | Código error | Condición |
|-------------|--------------|-----------|
| 400 | `PERIODO_INVALIDO` | Formato inválido o `periodoDesde > periodoHasta` |
| 400 | `VALIDACION` | Parámetros obligatorios faltantes |

---

## 6. Notas técnicas importantes

### Idempotency-Key

- El cliente debe generar una clave única por operación de negocio (UUID recomendado).
- El frontend actual genera `crypto.randomUUID()` en cada envío del formulario.
- Misma clave + mismo endpoint → respuesta original sin efectos duplicados (saldo, eventos).
- La clave se persiste en `idempotencia_aporte` y redundante en `aporte.idempotencia_key` (`UNIQUE`).

### Periodos `desde` / `hasta`

- El API usa rango `periodoDesde`–`periodoHasta` (formato `YYYY-MM`), no un solo parámetro `periodo`.
- Permite consolidados multi-mes sin cambiar el contrato.
- El frontend envía el mismo valor en ambos parámetros para consultas de un solo mes.
- La comparación de periodos es lexicográfica sobre `YYYY-MM` (válido para orden cronológico).

### Workarounds y consideraciones

| Tema | Detalle |
|------|---------|
| **Proxy Vite** | El frontend en dev usa proxy `/api → localhost:8082`. No hay CORS configurado en backend; sin proxy hay que exponer API directa y agregar CORS o usar `VITE_API_URL`. |
| **Claim huérfano** | Si el proceso cae tras el claim `EN_PROCESO` pero antes de `completar`, la clave queda bloqueada hasta limpieza manual o TTL (no implementado en v1). |
| **Concurrencia saldo** | Hasta 3 reintentos automáticos ante `OptimisticLockingFailureException`; luego `409`. |
| **Periodo del aporte** | Se calcula con fecha del sistema (`ClockPort`), no lo envía el cliente. |
| **JPA `validate`** | El esquema debe coincidir con Flyway; no usar `ddl-auto=update` en producción. |

---

*Última actualización: entrega v1 — registro idempotente, consolidado, frontend mínimo.*
