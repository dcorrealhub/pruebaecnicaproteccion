# Guía de proceso y pruebas — Reto B: Aportes Voluntarios

> **Proyecto:** CIS Protección S.A. — Prueba técnica AI-First  
> **Rama:** `candidato/andres-giraldo`  
> **Stack:** Spring Boot 3.4.1 · Java 21 · PostgreSQL 15 · React 18 · Vite

---

## 1. ¿Qué hace el sistema?

El sistema gestiona el registro de **aportes voluntarios** de afiliados a un fondo. Implementa cuatro flujos principales:

### 1.1 Registro de afiliados
Un afiliado debe existir en el catálogo antes de poder registrar aportes. Cada afiliado tiene un ID sintético (ej: `AF-001`), un nombre y un estado (`ACTIVO` / `BLOQUEADO`).

### 1.2 Ciclo de vida de un aporte

```
                   monto > umbral
                  ┌──────────────► EN_REVISION ──► APROBADO
 POST /api/aportes │                             └► RECHAZADO
                  └──────────────► PENDIENTE ────► APROBADO
                   monto ≤ umbral
```

| Estado | Cuándo ocurre |
|---|---|
| `PENDIENTE` | El monto no supera el umbral de revisión |
| `EN_REVISION` | El monto supera el umbral; requiere aprobación manual |
| `APROBADO` | Un operador aprobó el aporte |
| `RECHAZADO` | Un operador rechazó el aporte |

Cada transición de estado queda registrada en `revision_aporte` con el revisor, la decisión y el comentario.

### 1.3 Topes y umbrales configurables

Los límites operativos se guardan en `historico_parametros` con auditoría completa:

| Parámetro | Default | Descripción |
|---|---|---|
| `tope_mensual` | 10,000,000 COP | Máximo que un afiliado puede aportar en un mes |
| `umbral_revision` | 5,000,000 COP | Monto a partir del cual el aporte va a revisión |

Al primer arranque del sistema los valores se siembran desde las variables de entorno. Los cambios posteriores se hacen vía API y cada cambio queda como un registro histórico inmutable.

### 1.4 Idempotencia

El registro de aportes es idempotente: si se envía la misma `idempotenciaKey`, el sistema retorna el aporte original sin crear duplicado. El cliente debe generar esta clave con `crypto.randomUUID()` o equivalente.

---

## 2. Arquitectura

```
reto-b/
├── backend/                        Spring Boot 3.4.1 — puerto 8082
│   └── src/main/java/.../retob/
│       ├── domain/                 Núcleo puro: modelos, enums, excepciones, puertos
│       │   ├── model/              Aporte, Afiliado, SaldoMensual, ParametrosFondo…
│       │   ├── exception/          TopeMensualExcedidoException, etc.
│       │   └── port/               in/ (casos de uso) · out/ (repositorios)
│       ├── application/            Lógica de negocio — implementa los puertos de entrada
│       │   ├── usecase/            RegistrarAporteUseCaseImpl, CambiarEstadoAporteUseCaseImpl…
│       │   └── init/               ParametrosInicializador (siembra al arrancar)
│       └── infrastructure/         Adaptadores de entrada y salida
│           ├── persistence/        JPA entities, Spring Data repos, adapters
│           └── web/                Controllers, DTOs, GlobalExceptionHandler, OpenApiConfig
└── frontend/                       React 18 + Vite — puerto 5173
    └── src/
        ├── api/aportesApi.js       Fetch nativo hacia /api/*
        └── components/             RegistrarAporte, ConsolidadoAportes
```

**Regla de dependencia (Clean Architecture):** `domain` no importa nada de `application` ni `infrastructure`. Los adaptadores implementan los puertos definidos en `domain`.

---

## 3. Requisitos previos

| Herramienta | Versión mínima | Verificar |
|---|---|---|
| Java | 21 | `java -version` |
| Maven | 3.9+ | `mvn -version` |
| Node.js | 20+ | `node -version` |
| Docker + Compose | cualquiera reciente | `docker compose version` |

> **⚠ Java 25 en el sistema:** Si `java -version` muestra Java 25 (incompatible con Lombok), prefijar todos los comandos Maven con `JAVA_HOME=/usr/lib/jvm/java-21-openjdk`.

---

## 4. Cómo levantar el proyecto

### Paso 1 — Base de datos

```bash
# Desde la raíz del repositorio
docker compose up -d
```

Levanta PostgreSQL 15 en `localhost:5433`. La base de datos `proteccion_reto` se crea automáticamente.

### Paso 2 — Backend

```bash
cd reto-b/backend
JAVA_HOME=/usr/lib/jvm/java-21-openjdk mvn spring-boot:run
```

Al arrancar, Flyway ejecuta las migraciones V1→V5 y el `ParametrosInicializador` siembra los topes iniciales en `historico_parametros`.

Indicadores de arranque exitoso en los logs:
```
Migrating schema "public" to version "5 - add revision aporte"
Started RetoBApplication in X.XXX seconds
```

### Paso 3 — Frontend

```bash
cd reto-b/frontend
npm install
npm run dev
```

Abre `http://localhost:5173`. El proxy de Vite redirige `/api/*` → `http://localhost:8082`.

### Variables de entorno (opcional)

| Variable | Default | Propósito |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5433/proteccion_reto` | Conexión PostgreSQL |
| `DB_USER` | `postgres` | Usuario DB |
| `DB_PASSWORD` | `postgres` | Contraseña DB |
| `SERVER_PORT` | `8082` | Puerto del backend |
| `APORTE_TOPE_MENSUAL` | `10000000` | Tope inicial (se persiste en DB al primer arranque) |
| `APORTE_UMBRAL_REVISION` | `5000000` | Umbral inicial (se persiste en DB al primer arranque) |

---

## 5. Documentación interactiva (Swagger)

Con el backend corriendo:

| URL | Descripción |
|---|---|
| `http://localhost:8082/swagger-ui/index.html` | UI interactiva — probar endpoints desde el navegador |
| `http://localhost:8082/v3/api-docs` | Especificación OpenAPI 3.0 en JSON (importable en Postman/Insomnia) |

La UI organiza los endpoints en tres grupos: **Aportes**, **Afiliados** y **Parámetros del fondo**.

---

## 6. Flujo de prueba funcional paso a paso

### 6.1 Crear un afiliado

```bash
curl -s -X POST http://localhost:8082/api/afiliados \
  -H "Content-Type: application/json" \
  -d '{"afiliadoId":"AF-001","nombre":"Juan Sintético"}' | jq
```

Respuesta esperada `201`:
```json
{
  "id": 1,
  "afiliadoId": "AF-001",
  "nombre": "Juan Sintético",
  "estado": "ACTIVO",
  "creadoEn": "2026-06-26T..."
}
```

### 6.2 Verificar parámetros activos

```bash
curl -s http://localhost:8082/api/parametros/actual | jq
```

Respuesta esperada `200`:
```json
{
  "id": 1,
  "topeMensual": 10000000.00,
  "umbralRevision": 5000000.00,
  "modificadoPor": "SYSTEM",
  "comentario": "Carga inicial desde variables de entorno"
}
```

### 6.3 Registrar un aporte normal (→ PENDIENTE)

Monto de 1,000,000 COP — por debajo del umbral de 5,000,000:

```bash
curl -s -X POST http://localhost:8082/api/aportes \
  -H "Content-Type: application/json" \
  -d '{
    "afiliadoId": "AF-001",
    "monto": 1000000,
    "canal": "APP_MOVIL",
    "idempotenciaKey": "k-001"
  }' | jq
```

Respuesta esperada `201`:
```json
{
  "id": 1,
  "estado": "PENDIENTE",
  "monto": 1000000,
  "canal": "APP_MOVIL"
}
```

### 6.4 Registrar un aporte sobre el umbral (→ EN_REVISION automático)

Monto de 6,000,000 COP — supera el umbral de 5,000,000:

```bash
curl -s -X POST http://localhost:8082/api/aportes \
  -H "Content-Type: application/json" \
  -d '{
    "afiliadoId": "AF-001",
    "monto": 6000000,
    "canal": "WEB",
    "idempotenciaKey": "k-002"
  }' | jq
```

Respuesta esperada `201`:
```json
{
  "id": 2,
  "estado": "EN_REVISION"
}
```

### 6.5 Aprobar el aporte en revisión

```bash
curl -s -X PATCH http://localhost:8082/api/aportes/2/estado \
  -H "Content-Type: application/json" \
  -d '{
    "nuevoEstado": "APROBADO",
    "revisor": "OP-01",
    "comentario": "Monto verificado con el afiliado"
  }' | jq
```

Respuesta esperada `200`:
```json
{
  "id": 2,
  "estado": "APROBADO"
}
```

### 6.6 Ver historial de revisiones del aporte

```bash
curl -s http://localhost:8082/api/aportes/2/revisiones | jq
```

Respuesta esperada `200`:
```json
[
  {
    "id": 1,
    "revisor": "OP-01",
    "decision": "APROBADO",
    "comentario": "Monto verificado con el afiliado",
    "ocurridoEn": "2026-06-26T..."
  }
]
```

### 6.7 Consultar el consolidado del periodo

```bash
curl -s "http://localhost:8082/api/aportes/consolidado?afiliadoId=AF-001&periodoDesde=2026-06&periodoHasta=2026-06" | jq
```

Respuesta esperada `200`:
```json
{
  "afiliadoId": "AF-001",
  "totalAportado": 7000000.0,
  "detalle": [
    { "id": 1, "monto": 1000000, "estado": "PENDIENTE" },
    { "id": 2, "monto": 6000000, "estado": "APROBADO" }
  ]
}
```

### 6.8 Verificar el tope mensual

Con 7,000,000 acumulados, un aporte de 4,000,000 supera el tope de 10,000,000:

```bash
curl -s -X POST http://localhost:8082/api/aportes \
  -H "Content-Type: application/json" \
  -d '{
    "afiliadoId": "AF-001",
    "monto": 4000000,
    "canal": "SUCURSAL",
    "idempotenciaKey": "k-003"
  }' | jq
```

Respuesta esperada `422`:
```json
{
  "error": "TOPE_MENSUAL_EXCEDIDO",
  "mensaje": "Tope mensual excedido: tope=10000000.00, acumulado=7000000.00, monto solicitado=4000000"
}
```

### 6.9 Actualizar los topes en caliente (sin reiniciar)

```bash
curl -s -X POST http://localhost:8082/api/parametros \
  -H "Content-Type: application/json" \
  -d '{
    "topeMensual": 12000000,
    "umbralRevision": 6000000,
    "modificadoPor": "ADMIN",
    "comentario": "Ajuste Q2 2026 — aprobado por comité"
  }' | jq
```

El próximo aporte ya usará los nuevos topes. Ver el historial completo:
```bash
curl -s http://localhost:8082/api/parametros/historial | jq
```

### 6.10 Probar idempotencia

Reenviar la misma `idempotenciaKey` devuelve el aporte original sin duplicar:
```bash
curl -s -X POST http://localhost:8082/api/aportes \
  -H "Content-Type: application/json" \
  -d '{
    "afiliadoId": "AF-001",
    "monto": 1000000,
    "canal": "APP_MOVIL",
    "idempotenciaKey": "k-001"
  }' | jq
# Retorna el mismo aporte con id=1 — no crea uno nuevo
```

---

## 7. Pruebas de error — validaciones y casos borde

### 7.1 Campo obligatorio vacío → 400

```bash
curl -s -X POST http://localhost:8082/api/aportes \
  -H "Content-Type: application/json" \
  -d '{"afiliadoId":"","monto":100,"canal":"WEB","idempotenciaKey":"k-x"}' | jq
```
```json
{
  "error": "VALIDACION_FALLIDA",
  "campos": { "afiliadoId": "El afiliadoId es obligatorio" }
}
```

### 7.2 Enum inválido en el body → 400

```bash
curl -s -X POST http://localhost:8082/api/aportes \
  -H "Content-Type: application/json" \
  -d '{"afiliadoId":"AF-001","monto":100,"canal":"TELEGRAMA","idempotenciaKey":"k-x"}' | jq
```
```json
{
  "error": "FORMATO_INVALIDO",
  "mensaje": "El cuerpo de la solicitud contiene un valor inválido..."
}
```

### 7.3 Formato de periodo incorrecto → 400

```bash
curl -s "http://localhost:8082/api/aportes/consolidado?afiliadoId=AF-001&periodoDesde=26-06&periodoHasta=2026-06" | jq
```
```json
{
  "error": "VALIDACION_FALLIDA",
  "campos": { "periodoDesde": "El periodo debe tener formato YYYY-MM" }
}
```

### 7.4 PathVariable no numérico → 400

```bash
curl -s -X PATCH http://localhost:8082/api/aportes/abc/estado \
  -H "Content-Type: application/json" \
  -d '{"nuevoEstado":"APROBADO","revisor":"OP-01"}' | jq
```
```json
{
  "error": "TIPO_INVALIDO",
  "mensaje": "El parámetro 'id' debe ser de tipo Long."
}
```

### 7.5 Transición de estado inválida → 400

```bash
# Intentar pasar un aporte APROBADO a EN_REVISION (estado terminal)
curl -s -X PATCH http://localhost:8082/api/aportes/2/estado \
  -H "Content-Type: application/json" \
  -d '{"nuevoEstado":"EN_REVISION","revisor":"OP-01","comentario":"X"}' | jq
```
```json
{
  "error": "TRANSICION_INVALIDA",
  "mensaje": "Transición de estado inválida: APROBADO → EN_REVISION"
}
```

### 7.6 Umbral >= tope en actualización de parámetros → 409

```bash
curl -s -X POST http://localhost:8082/api/parametros \
  -H "Content-Type: application/json" \
  -d '{"topeMensual":5000000,"umbralRevision":6000000,"modificadoPor":"ADMIN"}' | jq
```
```json
{
  "error": "CONFLICTO",
  "mensaje": "El umbral de revisión (6000000) debe ser menor al tope mensual (5000000)."
}
```

---

## 8. Cómo ejecutar las pruebas automatizadas

### 8.1 Solo tests (sin DB)

Los tests unitarios y de controller (`@WebMvcTest`) no requieren PostgreSQL ni Docker.

```bash
cd reto-b/backend
JAVA_HOME=/usr/lib/jvm/java-21-openjdk mvn test
```

Resultado esperado:
```
Tests run: 70, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 8.2 Tests + reporte de cobertura JaCoCo

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk mvn clean verify
```

Genera el reporte HTML en:
```
target/site/jacoco/index.html   ← abrir en el navegador
target/site/jacoco/jacoco.xml   ← consumido por SonarQube
```

### 8.3 Tests + análisis SonarQube

Requiere SonarQube corriendo en `localhost:9000`.

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk mvn clean verify sonar:sonar \
  -Dsonar.projectKey=reto-b \
  -Dsonar.projectName='reto-b' \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=<tu-token>
```

Ver resultados en: `http://localhost:9000/dashboard?id=reto-b`

### 8.4 Métricas actuales de cobertura

| Métrica | Valor |
|---|---|
| Coverage total | **70.1 %** |
| Line coverage | 69.2 % |
| Branch coverage | **85.0 %** |
| Tests | **70** — 100 % éxito |

---

## 9. Qué cubre cada suite de tests

### Tests unitarios — sin Spring, sin DB (38 tests)

| Suite | Tests | Qué valida |
|---|---|---|
| `EstadoAporteTest` | 15 | Todas las transiciones válidas; `@ParameterizedTest` cubre APROBADO/RECHAZADO como estados terminales |
| `RegistrarAporteUseCaseImplTest` | 9 | Idempotencia, afiliado no encontrado, tope excedido, umbral exacto (límite), parámetros de DB vs defaults, verify de persistencia |
| `CambiarEstadoAporteUseCaseImplTest` | 6 | Not found, transición válida, `ArgumentCaptor` verifica revisor/decisión/timestamp, estados terminales no persisten |
| `ActualizarParametrosUseCaseImplTest` | 4 | Umbral igual/mayor al tope, caso válido, `ArgumentCaptor` verifica que `id=null` (lo asigna la DB) |
| `ConsultarAportesUseCaseImplTest` | 3 | Lista vacía → ZERO, suma BigDecimal con decimales sin pérdida de precisión, metadatos del query |

### Tests de controller — `@WebMvcTest` (32 tests)

| Suite | Tests | Qué valida |
|---|---|---|
| `AporteControllerTest` | 16 | POST: 201/EN_REVISION/validaciones/404/422; GET consolidado; PATCH: 200/404/400/TIPO_INVALIDO; GET revisiones |
| `AfiliadoControllerTest` | 8 | POST: 201/409 duplicado/400 campos; GET /{id}: 200/404; GET /: lista/vacía |
| `ParametrosControllerTest` | 8 | GET actual: 200/204; GET historial; POST: 201/409/400 campos nulos |

El `GlobalExceptionHandler` se carga automáticamente en `@WebMvcTest` — todos sus caminos de error quedan cubiertos sin levantar contexto de DB.

---

## 10. Mapa de endpoints

| Método | Ruta | Descripción | HTTP éxito |
|---|---|---|---|
| `POST` | `/api/afiliados` | Crear afiliado | 201 |
| `GET` | `/api/afiliados` | Listar afiliados | 200 |
| `GET` | `/api/afiliados/{id}` | Consultar afiliado | 200 |
| `POST` | `/api/aportes` | Registrar aporte (idempotente) | 201 |
| `GET` | `/api/aportes/consolidado` | Consolidado por periodo | 200 |
| `PATCH` | `/api/aportes/{id}/estado` | Cambiar estado + crear revisión | 200 |
| `GET` | `/api/aportes/{id}/revisiones` | Historial de revisiones | 200 |
| `GET` | `/api/parametros/actual` | Parámetros vigentes | 200 / 204 |
| `GET` | `/api/parametros/historial` | Historial de cambios | 200 |
| `POST` | `/api/parametros` | Actualizar topes | 201 |

---

## 11. Canales válidos y estados posibles

**Canales (`CanalOrigen`):** `APP_MOVIL` · `WEB` · `SUCURSAL`

**Ciclo de estados (`EstadoAporte`):**

```
                    ┌─────────────────────────────────────────┐
POST /api/aportes   │                                         │
    monto > umbral  ▼                                         │
PENDIENTE ──────► EN_REVISION ──► APROBADO (terminal)        │
    │                         └► RECHAZADO (terminal)        │
    └────────────────────────────► APROBADO (terminal) ◄─────┘
         monto ≤ umbral
```

Intentar cualquier otra transición devuelve `400 TRANSICION_INVALIDA`.

---

## 12. Consideraciones de seguridad

- Todo campo monetario usa `BigDecimal` con `NUMERIC(15,2)` en la DB — sin pérdida de precisión por punto flotante
- Idempotencia garantizada por `UNIQUE INDEX` en `idempotencia_key` — el segundo INSERT falla a nivel de DB aunque llegue concurrentemente
- Control de concurrencia en `saldo_mensual` vía `@Version` (optimistic locking) — dos hilos que actualicen el mismo saldo reciben `409 CONFLICTO_CONCURRENCIA`
- Race condition en inicialización de saldo resuelta con `REQUIRES_NEW` en `SaldoInicializadorAdapter` — el hilo perdedor relee el registro ya insertado
- Historial de parámetros es append-only — ningún registro se modifica ni elimina, solo se insertan nuevos
- Historial de revisiones es append-only — trazabilidad completa del ciclo de vida de cada aporte
