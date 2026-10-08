# Reto B · Aportes Voluntarios

Registro y consulta de aportes voluntarios de un afiliado (id sintético) a un fondo, sobre el stack
del CIS: **Spring Boot 3.4 / Java 21** (Clean Architecture + PostgreSQL + Flyway) y **React 18 / Vite 6**.

El contexto, los estándares y las reglas de negocio están fijados en [`AGENTS.md`](./AGENTS.md).
Este README cubre cómo correrlo, las decisiones de arquitectura y las buenas prácticas aplicadas.

---

## 1. Cómo correrlo

### Requisitos
- **JDK 21+** (ver nota de JDK 26 más abajo). No necesitás Maven: el proyecto trae Maven Wrapper (`mvnw`).
- **Docker** (para PostgreSQL) o un PostgreSQL local equivalente.
- **Node 18+** y npm (para el frontend).

### Opción A — Todo con Docker (recomendada)

Desde la **raíz del repositorio** (no desde `reto-b/`):

```bash
docker compose up --build
```

Levanta los tres servicios en una sola red:

| Servicio | Imagen | Puerto host | Notas |
|----------|--------|-------------|-------|
| `postgres` | postgres:15-alpine | 5432 | Crea la base `proteccion_reto` vacía al primer arranque |
| `backend`  | build de `reto-b/backend` (JDK 21) | 8082 | Espera a que Postgres esté *healthy*; Flyway migra el esquema |
| `frontend` | build de `reto-b/frontend` (Nginx) | 8080 | Sirve el bundle y proxya `/api` → `backend:8082` |

Una vez arriba: UI en `http://localhost:8080`, API en `http://localhost:8082`.

**La base de datos no requiere pasos manuales.** El contenedor `postgres` se autoconfigura con
`POSTGRES_DB`/`POSTGRES_USER`/`POSTGRES_PASSWORD` (definidos en el compose) y arranca con la base vacía.
Las **tablas las crea Flyway** cuando el backend arranca (no Postgres ni Hibernate). Dentro de la red de
compose el backend se conecta a `postgres:5432` (nombre del servicio), vía la variable
`SPRING_DATASOURCE_URL` del compose — por eso no usa `localhost` en ese modo. Los datos persisten en el
volumen `postgres_data`.

Para parar todo: `docker compose down` (agregá `-v` para borrar también el volumen de datos).

### Opción B — Local (sin Docker para la app)

Útil para desarrollo con hot-reload. Solo la base va en Docker:

#### B.1. Base de datos

Desde la raíz:

```bash
docker compose up -d postgres
```

PostgreSQL 15 en `localhost:5432`, base `proteccion_reto`, usuario/clave `postgres`/`postgres`
(coinciden con `backend/src/main/resources/application.properties`). **Flyway** crea el esquema al
arrancar el backend; no uses `ddl-auto` para generar tablas.

#### B.2. Backend

```bash
cd reto-b/backend
./mvnw spring-boot:run        # Linux/macOS
.\mvnw.cmd spring-boot:run    # Windows PowerShell
```

API en `http://localhost:8082`. Usa el `localhost:5432` del `application.properties`.

Correr los tests:

```bash
./mvnw test
```

> **Windows**: si `JAVA_HOME` no está seteado, apuntalo a tu JDK antes de correr, p. ej.
> `$env:JAVA_HOME='C:\Program Files\Java\jdk-21'`.

#### B.3. Frontend

```bash
cd reto-b/frontend
npm install
npm run dev
```

UI en `http://localhost:5173`. Vite proxya `/api` → `http://localhost:8082`, así que levantá el
backend antes.

### 1.3. Probar la API a mano

```bash
# Registrar un aporte (idempotente: repetir con la misma idempotenciaKey no duplica)
curl -i -X POST http://localhost:8082/api/aportes \
  -H "Content-Type: application/json" \
  -d '{"afiliadoId":"AF-001","monto":"150000.00","fecha":"2025-03-10","canal":"WEB","idempotenciaKey":"demo-001"}'

# Consolidado por periodo
curl "http://localhost:8082/api/aportes/consolidado?afiliadoId=AF-001&periodoDesde=2025-01&periodoHasta=2025-12"
```

---

## 2. API

| Método | Ruta | Descripción | Éxito |
|--------|------|-------------|-------|
| `POST` | `/api/aportes` | Registra un aporte (idempotente) | `201 Created` + header `Location` |
| `GET`  | `/api/aportes/{id}` | Obtiene un aporte por id | `200` / `404` |
| `GET`  | `/api/aportes/consolidado?afiliadoId&periodoDesde&periodoHasta` | Total y detalle del periodo | `200` |

**Cuerpo de registro**: `afiliadoId`, `monto` (string decimal), `fecha` (`YYYY-MM-DD`, no futura),
`canal` (`APP_MOVIL` \| `WEB` \| `SUCURSAL`), `idempotenciaKey`.

### Códigos de error (contrato ProblemDetail, RFC 7807)

| HTTP | Cuándo |
|------|--------|
| `400` | Validación de formato (campos inválidos, enum de canal desconocido, JSON mal formado) |
| `404` | Aporte inexistente |
| `409` | Conflicto de concurrencia tras agotar reintentos |
| `422` | Violación de regla de negocio (monto ≤ 0, tope mensual excedido, fecha futura) |

Las respuestas de error no hacen eco del input ni filtran stacktraces.

---

## 3. Decisiones de arquitectura

### 3.1. Hexagonal / Clean Architecture
El dominio no conoce la infraestructura. Las dependencias apuntan hacia adentro:

```
infrastructure (web, persistence, config)
        │  implementa / invoca
        ▼
   application (casos de uso)
        │  depende de
        ▼
      domain (modelos puros + puertos + reglas)
```

- `domain/model` — POJOs puros (`Aporte`, `SaldoMensual`, `Canal`, `PoliticaAportes`), sin Spring ni JPA.
- `domain/port/in` — casos de uso (`RegistrarAporteUseCase`, `ConsultarAportesUseCase`).
- `domain/port/out` — puertos de salida (`AporteRepositoryPort`, `SaldoRepositoryPort`,
  `ParametrosAportePort`, `EventoAportePort`).
- `application/usecase` — orquestación; implementa los puertos de entrada.
- `infrastructure` — adaptadores JPA, controlador REST, resolución de parámetros. El mapeo
  entidad ↔ dominio vive en el adaptador (`AporteEntityMapper`), nunca se expone la entidad JPA en la API.

### 3.2. CQRS (separación comando / consulta)
`RegistrarAporteUseCase` es un **comando** (muta estado, transaccional); `ConsultarAportesUseCase`
es **consulta** (`@Transactional(readOnly = true)`). No se mezclan.

### 3.3. Reglas de negocio aisladas y configurables
`PoliticaAportes.evaluar(monto, canal, acumuladoMes, parametros)` concentra las reglas (monto > 0, tope
mensual inclusivo, umbral de revisión exclusivo y dependiente del canal) como función pura. Los
parámetros se reciben vía `ParametrosAportePort`, cuya firma es `resolver(afiliadoId, mes)`. Hoy la
implementación (`ParametrosAporteGlobalAdapter`) devuelve valores globales de configuración; pasar a
**parámetros por afiliado** es sustituir ese adaptador, sin tocar dominio ni casos de uso. Diseñado así a
propósito para absorber cambios de requisito (ver sección 5).

### 3.4. Idempotencia
La `idempotenciaKey` la genera el cliente. Hay una **restricción UNIQUE en BD** sobre ella. El caso de
uso primero consulta por la clave; ante una carrera entre dos peticiones simultáneas, el UNIQUE deja
insertar solo a una, y la otra recupera y devuelve el aporte original sin duplicar efectos.

### 3.5. Concurrencia del saldo mensual
Control **optimista** con `@Version` sobre `saldo_mensual`. Si dos aportes del mismo afiliado/mes
colisionan, Hibernate lanza `OptimisticLockingFailureException`; el caso de uso **reintenta** (hasta 3
veces, cada intento en su propia transacción vía `RegistrarAporteTransaccion`) y, si persiste, responde
`409`.

### 3.6. Dinero
`BigDecimal` en todas las capas, escala 2 con `RoundingMode.HALF_EVEN`, columnas `NUMERIC(19,2)`,
comparaciones con `compareTo`. En el frontend el monto viaja como **string** en el JSON para no perder
precisión (JavaScript usa punto flotante); el backend lo deserializa a `BigDecimal`.

### 3.7. Persistencia con Flyway
El esquema se gestiona solo por migraciones (`db/migration`), con `ddl-auto=validate`:
- `V1__init.sql` — esquema base (scaffold).
- `V2__montos_19_2_y_check_monto.sql` — amplía montos a `NUMERIC(19,2)` y agrega `CHECK (monto > 0)`.

Constraints en BD: `NOT NULL`, `CHECK (monto > 0)`, UNIQUE `(afiliado_id, mes)` en saldo, UNIQUE de
idempotencia. La tabla `evento_aporte` registra `APORTE_REGISTRADO` para trazabilidad.

---

## 4. Buenas prácticas aplicadas

- **Validación en dos capas**: Bean Validation en el borde (`@NotBlank`, `@NotNull`, `@DecimalMin`,
  `@Digits`, `@PastOrPresent`, enum de canal) **más** revalidación de invariantes en el dominio
  (el dominio no confía en el borde).
- **Manejo de errores centralizado** con `@RestControllerAdvice` y contrato ProblemDetail (RFC 7807);
  códigos HTTP correctos; sin fuga de detalles internos.
- **201 + `Location`** apuntando al recurso creado (`GET /api/aportes/{id}`).
- **Observabilidad**: filtro de `X-Correlation-Id` (toma o genera uno, lo propaga al MDC y a la
  respuesta) y patrón de log que incluye el id de correlación. Los logs no registran PII (monto, afiliado).
- **Tests enfocados en lo que importa**: 35 en total —
  dominio (límites del tope, umbral por canal, monto), caso de uso (idempotencia, reintento → 409, fecha
  futura, marcado por canal), integración de persistencia (UNIQUE de idempotencia, consolidado por rango,
  escala del monto, bloqueo optimista) y web (`@WebMvcTest`: 201+Location, 400/404/409/422).
- **DI por constructor explícito** (sin inyección por campo), lo que mantiene las clases testeables y
  las dependencias explícitas.

---

## 5. Cómo tratamos el ajuste de requisito (umbral por canal)

> **Requisito**: "Negocio nos avisa que los aportes por sucursal tienen mayor riesgo. A partir de ahora,
> un aporte por canal `SUCURSAL` se marca para revisión si supera 3.000.000. Los demás canales siguen
> con 5.000.000."

El diseño estaba preparado para esto (regla aislada en el dominio + parámetros resueltos por un puerto),
así que el ajuste fue **acotado y aditivo**, sin reescribir el flujo.

**Qué cambió:**
1. `ParametrosAporte` ahora modela un **umbral por defecto más overrides por canal**
   (`umbralRevisionDefault` + `Map<Canal, BigDecimal>`), con el método `umbralRevisionPara(canal)` que
   devuelve el override del canal si existe, o el default.
2. `PoliticaAportes.evaluar(...)` recibe el **`Canal`** y usa `umbralRevisionPara(canal)`. La regla sigue
   viviendo en el dominio, como función pura.
3. `ParametrosAporteGlobalAdapter` lee una nueva clave configurable
   `aporte.umbral-revision-sucursal` (default 3.000.000) y arma el mapa `{ SUCURSAL → 3.000.000 }`.

**Qué NO cambió** (y por qué eso valida el diseño):
- El **tope mensual**, la idempotencia, la concurrencia, la persistencia y la API quedaron intactos.
- El **contrato REST** no cambió: el canal ya viajaba en el request; la decisión de marcar es interna.
- El cambio es **configurable**, no hardcodeado: los umbrales (default y de sucursal) viven en
  `application.properties` y se pueden sobreescribir por entorno/variable sin recompilar.

**Decisión de diseño**: metí el `Canal` en la firma del dominio en vez de resolver el umbral afuera y
pasar un escalar. Así la regla "qué umbral aplica según canal" queda **en el dominio** (donde debe estar
según AGENTS.md), testeable de forma pura y en un solo lugar.

**Semántica confirmada**: el umbral es **exclusivo** — `SUCURSAL` con monto exactamente 3.000.000 NO se
marca; 3.000.001 sí. Un mismo monto de 4.000.000 se marca en `SUCURSAL` pero no en `WEB`/`APP_MOVIL`.

**Cobertura**: tests de dominio (`PoliticaAportesTest`) para los límites por canal y un test de caso de
uso (`RegistrarAporteTransaccionTest`) que verifica el marcado end-to-end de un aporte de sucursal.

**Extensión futura**: si mañana el umbral varía por afiliado además de por canal, solo cambia
`ParametrosAportePort` (su firma ya recibe `afiliadoId` y `mes`); el dominio no se toca.

---

## 6. Notas del entorno

- **Maven Wrapper**: el proyecto incluye `mvnw`/`mvnw.cmd` y el jar del wrapper (versionado), así que no
  necesitás Maven instalado. Primera ejecución descarga Maven 3.9.9.
- **JDK 26**: el proyecto fija Java 21, pero es compatible con toolchains más nuevos. Dos ajustes del
  `pom.xml` lo hacen funcionar bajo JDK 26: se fija **Byte Buddy 1.18.1** (necesario para que Mockito
  mockee bajo JDK 26) y **no se usa Lombok** (su generación de código no es fiable en JDK 26); las
  entidades y componentes usan constructores y accesores explícitos.

---

## 7. Qué falta para producción (fuera de alcance de la prueba)

- **Autenticación/autorización**: los endpoints hoy están abiertos. En un entorno SFC deben protegerse.
- **Parámetros por afiliado**: la estructura ya está lista (`ParametrosAportePort`); falta la fuente real.
- **Observabilidad completa**: métricas, trazas distribuidas y agregación de logs.
- **Idempotencia a escala**: expiración/retención de claves y verificación de que el payload del reintento
  coincide con el original.
