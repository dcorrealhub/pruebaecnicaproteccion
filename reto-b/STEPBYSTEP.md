# Reto B — Step-by-Step: Construcción asistida con IA

> **Rama:** `candidato/andres-giraldo`  
> **Stack:** Spring Boot 3.4.1 · Java 21 · PostgreSQL 15 · React 18 · Vite  
> **Herramienta IA:** Claude Sonnet 4.6 (Claude Code CLI)

---

## 1. Análisis del scaffold

El proyecto base entregaba toda la estructura de carpetas y contratos pero los casos de uso y
adaptadores lanzaban `UnsupportedOperationException`. Se leyó cada archivo antes de escribir una
sola línea para entender el contrato existente y no romperlo.

Archivos scaffold revisados y su estado inicial:

| Archivo | Estado inicial |
|---|---|
| `domain/model/Aporte.java` | Completo — sin `EstadoAporte`, `canal` era `String` |
| `domain/port/in/RegistrarAporteUseCase.java` | Completo — `canal` como `String` |
| `domain/port/out/AporteRepositoryPort.java` | Completo — sin `findById` |
| `application/usecase/RegistrarAporteUseCaseImpl.java` | TODO — lanzaba excepción |
| `application/usecase/ConsultarAportesUseCaseImpl.java` | TODO — lanzaba excepción |
| `infrastructure/persistence/adapter/JpaAporteRepositoryAdapter.java` | TODO — lanzaba excepción |
| `infrastructure/persistence/adapter/JpaSaldoRepositoryAdapter.java` | TODO — lanzaba excepción |
| `infrastructure/web/AporteController.java` | Completo — 2 endpoints |
| `infrastructure/web/dto/RegistrarAporteRequest.java` | Completo — `canal` como `String` |
| `infrastructure/web/dto/AporteResponse.java` | Completo — `marcadaRevision boolean` |
| `frontend/src/api/aportesApi.js` | TODO — lanzaba Error |
| `frontend/src/components/RegistrarAporte.jsx` | Estructura lista, lógica incompleta |

---

## 2. Decisiones de diseño tomadas

### 2.1 Enriquecimiento del dominio

| Decisión | Alternativa descartada | Razón |
|---|---|---|
| `EstadoAporte` enum con ciclo de vida PENDIENTE → EN_REVISION → APROBADO/RECHAZADO | Mantener `marcadaRevision boolean` | El boolean no captura el ciclo completo ni permite trazabilidad de revisiones |
| `CanalOrigen` enum (APP_MOVIL, WEB, SUCURSAL) | `String` libre | Evita valores inválidos en runtime; el frontend ya enviaba esos valores |
| Value objects solo en validación del Command, no dentro del agregado | VOs dentro de `Aporte` | Reduce fricción en el mapper JPA; `BigDecimal` y enums ya son tipados |

### 2.2 Parámetros configurables

| Decisión | Razón |
|---|---|
| Env vars (`${APORTE_TOPE_MENSUAL:10000000}`) como defaults | Cada entorno puede sobrescribir sin redeployar el schema |
| Tabla `historico_parametros` para valores activos | Permite auditoría completa de quién cambió los topes y cuándo |
| `ParametrosInicializador` siembra la tabla en el primer arranque | El sistema nunca arranca con la tabla vacía; los env vars son la fuente de verdad inicial |
| `ParametroRepository.findLatest()` lee el valor activo en tiempo real | Los nuevos topes aplican al siguiente aporte sin reiniciar el servicio |

### 2.3 Tabla de afiliados

La decisión de agregar `afiliado` como entidad propia (y no solo `afiliadoId: String` libre)
permite:
- Validar que el afiliado exista antes de aceptar un aporte
- Asociar estado `ACTIVO/BLOQUEADO` al afiliado
- Base para restricciones futuras (límites por estado, bloqueos por revisiones pendientes)

### 2.4 Historial de revisiones

`revision_aporte` es una tabla de solo inserción (append-only): cada cambio de estado
genera un registro con `revisor`, `decision` y `comentario`. No se modifica ni elimina.
Esto garantiza trazabilidad completa del ciclo de vida de cada aporte.

### 2.5 Arquitectura — Clean Architecture

```
domain/          ← núcleo puro: modelos, enums, excepciones, puertos (interfaces)
application/     ← casos de uso: lógica de negocio, orquestación
infrastructure/  ← adapters: JPA, Spring Data, REST controllers, DTOs
```

La regla de dependencia es estricta: `domain` no importa nada de `application` ni de
`infrastructure`. Los adaptadores implementan los puertos definidos en `domain`.

### 2.6 Concurrencia — lock optimista

`saldo_mensual` tiene columna `version INTEGER` mapeada con `@Version` en `SaldoMensualEntity`.
Si dos transacciones concurrentes intentan actualizar el mismo saldo mensual, JPA lanza
`ObjectOptimisticLockingFailureException` → el `GlobalExceptionHandler` la convierte en HTTP 409.

---

## 3. Paso a paso de la implementación

### Paso 1 — Excepciones de dominio

Creadas en `domain/exception/`:

```
TopeMensualExcedidoException   — cuando monto + acumulado > tope mensual
TransicionEstadoInvalidaException — cuando se intenta una transición de estado ilegal
AporteNotFoundException        — cuando no existe un aporte con el id solicitado
AfiliadoNotFoundException      — cuando no existe el afiliadoId en la tabla
```

Todas extienden `RuntimeException` para no obligar al llamador a capturarlas. El
`GlobalExceptionHandler` las intercepta y las convierte a respuestas HTTP semánticas.

### Paso 2 — Enums y ciclo de vida

**`EstadoAporte`** implementa la lógica de transición directamente en el enum:

```java
public EstadoAporte transicionar(EstadoAporte nuevo) {
    if (!TRANSICIONES_VALIDAS.get(this).contains(nuevo)) {
        throw new TransicionEstadoInvalidaException(this, nuevo);
    }
    return nuevo;
}
```

Mapa de transiciones válidas:

```
PENDIENTE   → EN_REVISION, APROBADO
EN_REVISION → APROBADO, RECHAZADO
APROBADO    → (ninguna)
RECHAZADO   → (ninguna)
```

Intentar cualquier otra transición lanza `TransicionEstadoInvalidaException`.

### Paso 3 — Dominio enriquecido

**`Aporte.java`** — modificaciones sobre el scaffold:
- `boolean marcadaRevision` → eliminado
- `String canal` → `CanalOrigen canal` (enum)
- Añadido `EstadoAporte estado`

**Nuevos modelos:**
- `Afiliado` — id, afiliadoId, nombre, EstadoAfiliado, creadoEn
- `ParametrosFondo` — id, topeMensual, umbralRevision, modificadoPor, modificadoEn, comentario
- `RevisionAporte` — id, aporteId, revisor, decision (EstadoAporte), comentario, ocurridoEn

### Paso 4 — Nuevos puertos

**Entrada (casos de uso nuevos):**
- `CambiarEstadoAporteUseCase` — valida transición, guarda revisión
- `ConsultarRevisionesUseCase` — historial de revisiones por aporte
- `RegistrarAfiliadoUseCase` — crea un afiliado con estado ACTIVO
- `ConsultarAfiliadoUseCase` — consulta individual y lista completa
- `ActualizarParametrosUseCase` — inserta nuevo registro en historico_parametros
- `ConsultarParametrosUseCase` — valor actual e historial

**Salida (repositorios nuevos):**
- `RevisionRepository` — guardar, findByAporteId
- `AfiliadoRepository` — guardar, findByAfiliadoId, findAll
- `ParametroRepository` — guardarCambio, findLatest, findAll

`AporteRepositoryPort` existente: añadido `findById(Long)` para que
`CambiarEstadoAporteUseCaseImpl` pueda buscar el aporte antes de cambiar su estado.

### Paso 5 — Migraciones Flyway

Todas en `src/main/resources/db/migration/`:

| Versión | Cambio |
|---|---|
| `V1__init.sql` | Scaffold original: `saldo_mensual`, `aporte`, `evento_aporte` |
| `V2__add_estado_aporte.sql` | DROP `marcada_revision`, ADD `estado VARCHAR(20) DEFAULT 'PENDIENTE'` |
| `V3__add_afiliado.sql` | Tabla `afiliado` con índice en `afiliado_id` |
| `V4__add_historico_parametros.sql` | Tabla `historico_parametros` con `NUMERIC(15,2)` para dinero |
| `V5__add_revision_aporte.sql` | Tabla `revision_aporte` con FK a `aporte(id)` |

Flyway aplica las migraciones en orden al arrancar; si la base de datos ya tiene V1-V2
aplicadas, solo ejecuta las pendientes.

### Paso 6 — Entidades JPA y Spring Data repos

**`AporteEntity`** — actualizada:
- `boolean marcadaRevision` → `EstadoAporte estado` con `@Enumerated(EnumType.STRING)`
- `String canal` → `CanalOrigen canal` con `@Enumerated(EnumType.STRING)`

**Nuevas entidades:**
- `AfiliadoEntity` — mapea `afiliado`
- `ParametrosFondoEntity` — mapea `historico_parametros`; `@PrePersist` asigna `modificadoEn`
- `RevisionAporteEntity` — `@ManyToOne(fetch=LAZY)` a `AporteEntity`

**Spring Data repos nuevos:**
- `SpringDataAfiliadoRepository` — `findByAfiliadoId`
- `SpringDataParametrosRepository` — `findTopByOrderByModificadoEnDesc()` para obtener el último
- `SpringDataRevisionRepository` — `findByAporteId`

### Paso 7 — Adaptadores JPA (5 implementados)

Patrón uniforme en todos los adaptadores:

```
toEntity(Domain) → mapea del modelo de dominio a entidad JPA
toDomain(Entity) → mapea de entidad JPA al modelo de dominio
```

Ningún adaptador filtra ni aplica lógica de negocio. Son traducciones puras.

**`JpaSaldoRepositoryAdapter.inicializar()`**: crea saldo con `total=BigDecimal.ZERO`
y `version=0` para que el lock optimista arranque en la versión correcta.

**`JpaRevisionRepositoryAdapter`**: para evitar cargar la entidad `AporteEntity` completa
al guardar una revisión, se crea una referencia proxy (`new AporteEntity(); .setId(id)`)
que JPA entiende como FK sin hacer SELECT adicional.

### Paso 8 — Casos de uso

#### `RegistrarAporteUseCaseImpl.registrar()` — flujo completo:

```
1. Idempotencia
   findByIdempotenciaKey(key)
   → Si existe: return aporte existente (sin errores, sin duplicados)

2. Validación del afiliado
   afiliadoRepository.findByAfiliadoId(id)
   → Si no existe: throw AfiliadoNotFoundException → HTTP 404

3. Parámetros activos
   parametroRepository.findLatest()
   → Si tabla vacía: usa defaults de @Value (env vars)

4. Saldo mensual
   saldoRepository.findByAfiliadoIdAndMes(id, periodo)
   → Si no existe: saldoRepository.inicializar(id, periodo)

5. Validación tope mensual
   nuevoTotal = saldo.calcularNuevoTotal(monto)
   → Si nuevoTotal > topeMensual: throw TopeMensualExcedidoException → HTTP 422

6. Estado automático
   monto > umbralRevision → EN_REVISION
   monto ≤ umbralRevision → PENDIENTE

7. Persistencia del aporte
   aporteRepository.guardar(aporte)

8. Actualización del saldo (lock optimista)
   saldoRepository.guardar(saldo.conTotal(nuevoTotal))
   → Concurrencia: ObjectOptimisticLockingFailureException → HTTP 409
```

#### `CambiarEstadoAporteUseCaseImpl.cambiar()` — flujo:

```
1. findById(id)       → AporteNotFoundException si no existe
2. estado.transicionar(nuevo) → TransicionEstadoInvalidaException si ilegal
3. guardar aporte con nuevo estado
4. guardar RevisionAporte (registro inmutable de auditoría)
```

### Paso 9 — `ParametrosInicializador`

Componente Spring que escucha `ApplicationReadyEvent` (se dispara cuando el contexto
está completamente listo, incluyendo Flyway y el pool de conexiones):

```java
@EventListener(ApplicationReadyEvent.class)
@Transactional
public void sembrarParametrosIniciales() {
    if (parametroRepository.findLatest().isEmpty()) {
        parametroRepository.guardarCambio(new ParametrosFondo(
            null, topeMensualDefault, umbralRevisionDefault,
            "SYSTEM", OffsetDateTime.now(), "Carga inicial desde variables de entorno"
        ));
    }
}
```

Solo actúa si la tabla está vacía → idempotente: reiniciar el servidor no duplica el registro.

### Paso 10 — REST API

#### Endpoints implementados

| Método | Ruta | Descripción | HTTP OK |
|---|---|---|---|
| POST | `/api/afiliados` | Registrar afiliado | 201 |
| GET | `/api/afiliados` | Listar todos los afiliados | 200 |
| GET | `/api/afiliados/{afiliadoId}` | Consultar afiliado | 200 |
| POST | `/api/aportes` | Registrar aporte (idempotente) | 201 |
| GET | `/api/aportes/consolidado` | Consolidado por periodo | 200 |
| PATCH | `/api/aportes/{id}/estado` | Cambiar estado con revisión | 200 |
| GET | `/api/aportes/{id}/revisiones` | Historial de revisiones | 200 |
| GET | `/api/parametros/actual` | Parámetros vigentes | 200 / 204 |
| GET | `/api/parametros/historial` | Historial de cambios | 200 |
| POST | `/api/parametros` | Actualizar topes/umbrales | 201 |

---

## 4. Manejo de errores y validaciones

### 4.1 Validación de entrada (`@Valid` + Bean Validation)

Todos los requests pasan por `@Valid`. Los campos `@NotBlank`, `@NotNull`, `@DecimalMin`
se validan automáticamente antes de entrar al controller.

| Anotación | Dónde se usa |
|---|---|
| `@NotBlank` | afiliadoId, canal, idempotenciaKey, revisor, modificadoPor |
| `@NotNull` | monto, canal (enum), nuevoEstado, topeMensual, umbralRevision |
| `@DecimalMin("0.01")` | monto, topeMensual, umbralRevision |

### 4.2 `GlobalExceptionHandler` — mapeo de excepciones a HTTP

| Excepción | HTTP | Código de error |
|---|---|---|
| `TopeMensualExcedidoException` | **422** Unprocessable Entity | `TOPE_MENSUAL_EXCEDIDO` |
| `TransicionEstadoInvalidaException` | **400** Bad Request | `TRANSICION_INVALIDA` |
| `AporteNotFoundException` | **404** Not Found | `APORTE_NO_ENCONTRADO` |
| `AfiliadoNotFoundException` | **404** Not Found | `AFILIADO_NO_ENCONTRADO` |
| `IllegalArgumentException` | **409** Conflict | `CONFLICTO` |
| `OptimisticLockException` | **409** Conflict | `CONFLICTO_CONCURRENCIA` |
| `ObjectOptimisticLockingFailureException` | **409** Conflict | `CONFLICTO_CONCURRENCIA` |
| `MethodArgumentNotValidException` | **400** Bad Request | `VALIDACION_FALLIDA` |

### 4.3 Formato de respuesta de error

Errores simples:
```json
{
  "error": "TOPE_MENSUAL_EXCEDIDO",
  "mensaje": "Tope mensual excedido: tope=10000000.00, acumulado=7000000.00, monto solicitado=4000000"
}
```

Errores de validación (incluye detalle por campo):
```json
{
  "error": "VALIDACION_FALLIDA",
  "mensaje": "Uno o más campos no son válidos",
  "campos": {
    "monto": "El monto debe ser mayor a cero",
    "afiliadoId": "El afiliadoId es obligatorio"
  }
}
```

### 4.4 Reglas de negocio aplicadas en el dominio

| Regla | Dónde se valida |
|---|---|
| Monto > 0 | Bean Validation en `RegistrarAporteRequest` |
| Afiliado existente | `RegistrarAporteUseCaseImpl` → `AfiliadoNotFoundException` |
| No superar tope mensual | `RegistrarAporteUseCaseImpl` → `TopeMensualExcedidoException` |
| Marcar para revisión si > umbral | `RegistrarAporteUseCaseImpl` → estado `EN_REVISION` automático |
| Idempotencia por `idempotenciaKey` | `RegistrarAporteUseCaseImpl` → return early |
| Transiciones de estado válidas | `EstadoAporte.transicionar()` → `TransicionEstadoInvalidaException` |
| Concurrencia saldo mensual | `@Version` en `SaldoMensualEntity` → `OptimisticLockException` |

---

## 5. Variables de entorno

Todas las propiedades sensibles se configuran vía env vars con defaults seguros para local:

| Variable | Default | Uso |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5433/proteccion_reto` | Conexión a PostgreSQL |
| `DB_USER` | `postgres` | Usuario de BD |
| `DB_PASSWORD` | `postgres` | Contraseña de BD |
| `SERVER_PORT` | `8082` | Puerto del backend |
| `APORTE_TOPE_MENSUAL` | `10000000` | Tope mensual inicial (se persiste en BD) |
| `APORTE_UMBRAL_REVISION` | `5000000` | Umbral que activa revisión (se persiste en BD) |

---

## 6. Esquema de base de datos final

```
saldo_mensual
  id, afiliado_id, mes (YYYY-MM), total NUMERIC(15,2), version INTEGER

aporte
  id, afiliado_id, monto NUMERIC(15,2), fecha, canal VARCHAR(50),
  periodo (YYYY-MM), estado VARCHAR(20), idempotencia_key, creado_en

evento_aporte
  id, aporte_id FK, tipo VARCHAR(50), ocurrido_en

afiliado
  id, afiliado_id UNIQUE, nombre, estado VARCHAR(20), creado_en

historico_parametros
  id, tope_mensual NUMERIC(15,2), umbral_revision NUMERIC(15,2),
  modificado_por, modificado_en, comentario

revision_aporte
  id, aporte_id FK → aporte(id), revisor, decision VARCHAR(20),
  comentario, ocurrido_en
```

---

## 7. Frontend — aportesApi.js

Implementado con `fetch` nativo (sin axios ni librerías adicionales).

```javascript
// registrarAporte: POST con JSON, lanza Error si status no-ok
// consultarConsolidado: GET con URLSearchParams para query string
```

El proxy de Vite (`vite.config.js`) redirige `/api/*` → `http://localhost:8082`
en desarrollo, evitando CORS.

En producción (Dockerfile + nginx), el reverse proxy en `nginx.conf` hace el mismo
redireccionamiento desde el contenedor frontend al servicio `backend:8082`.

---

## 8. Cómo ejecutar el proyecto

### Requisitos
- Java 21 (`JAVA_HOME=/usr/lib/jvm/java-21-openjdk` si el sistema tiene Java más nuevo)
- Maven 3.9+
- Node 20+
- Docker + Docker Compose

### Arranque

```bash
# 1. Base de datos (PostgreSQL en puerto 5433)
docker compose up -d

# 2. Backend (Flyway aplica V1→V5 automáticamente)
cd reto-b/backend
JAVA_HOME=/usr/lib/jvm/java-21-openjdk mvn spring-boot:run

# 3. Frontend (proxy a localhost:8082)
cd reto-b/frontend
npm install
npm run dev    # http://localhost:5173
```

### Smoke test rápido

```bash
# Crear afiliado
curl -s -X POST http://localhost:8082/api/afiliados \
  -H "Content-Type: application/json" \
  -d '{"afiliadoId":"AF-001","nombre":"Juan Sintético"}'

# Registrar aporte (< umbral → PENDIENTE)
curl -s -X POST http://localhost:8082/api/aportes \
  -H "Content-Type: application/json" \
  -d '{"afiliadoId":"AF-001","monto":1000000,"canal":"APP_MOVIL","idempotenciaKey":"k-001"}'

# Registrar aporte (> umbral → EN_REVISION automático)
curl -s -X POST http://localhost:8082/api/aportes \
  -H "Content-Type: application/json" \
  -d '{"afiliadoId":"AF-001","monto":6000000,"canal":"WEB","idempotenciaKey":"k-002"}'

# Consultar parámetros activos (sembrados desde env vars al primer arranque)
curl -s http://localhost:8082/api/parametros/actual
```

---

## 9. Próximos pasos

- [ ] **Swagger / OpenAPI** — `springdoc-openapi-starter-webmvc-ui` + anotaciones `@Operation` / `@Tag`
- [ ] **Tests unitarios** — `EstadoAporte` (transiciones inválidas), `RegistrarAporteUseCaseImpl` (3 caminos: normal, revisión, tope)
- [ ] **Tests de integración** — `@DataJpaTest` para adaptadores, `@SpringBootTest` + MockMvc para endpoints
- [ ] **Gestión de estado del afiliado** — bloquear aportes si `EstadoAfiliado == BLOQUEADO`
