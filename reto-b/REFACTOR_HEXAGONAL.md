# Refactorización a Arquitectura Hexagonal — Reto B

Documento de la refactorización del backend de **aportes voluntarios** a arquitectura
hexagonal (clean architecture), siguiendo las convenciones del scaffold de referencia.

> **Objetivo:** llevar el backend a una estructura de capas limpia (dominio puro,
> handlers, driven adapter, entrypoint) con MapStruct, cableado manual de casos de uso,
> excepciones de dominio y calidad (Swagger, Actuator, JaCoCo) — **sin romper el contrato
> HTTP** (frontend y colección Postman siguen funcionando sin cambios).

## 1. Alcance

| Incluido | Excluido (a propósito) |
|---|---|
| Reestructuración de capas (dominio / application / infrastructure) | Seguridad JWT / autenticación |
| MapStruct para todo el mapeo (DTO ↔ dominio ↔ entity) | Rate limiting (Bucket4j) |
| Casos de uso puros cableados en `BeanConfiguration` | Cambio de versión de Spring Boot (se mantuvo **3.4.1**) |
| `BaseEntity` (auditoría `created_at` / `updated_at`) | |
| Swagger/OpenAPI, Actuator, JaCoCo con gate de cobertura | |

## 2. Estructura antes → después

**Antes** (hexagonal "plano"):
```
domain/model, domain/port/in, domain/port/out
application/usecase (@Service, con lógica + TransactionTemplate)
infrastructure/persistence (entity, repository, adapter con mapeo manual)
infrastructure/web (controllers, dto, GlobalExceptionHandler)
```

**Después** (estilo scaffold):
```
domain/
  model/aporte/{Aporte, EstadoAporte, SaldoMensual, ConsolidadoAportes, ConsultaConsolidado}
  model/aporte/gateway/{AporteOutputPort, SaldoOutputPort, EventoOutputPort}
  model/parametro/{ParametrosAporte} + gateway/ParametroOutputPort
  usecase/{AporteUseCase, ParametroUseCase}          ← puro, sin Spring
  usecase/input/{AporteInputPort, ParametroInputPort}
  constant/{AporteMessages, ParametroMessages}
  exception/{DomainNotFoundException, DomainValidationException, DomainBusinessRuleException}
application/
  handler/{AporteHandler, ParametroHandler}
  handler/adapter/{AporteHandlerAdapter, ParametroHandlerAdapter}   ← @Service @Transactional
infrastructure/
  config/{BeanConfiguration, OpenApiConfiguration}
  drivenadapter/postgresql/
    entity/{BaseEntity, AporteEntity, SaldoMensualEntity, ParametroAporteEntity, EventoAporteEntity}
    repository/{AporteRepository, SaldoRepository, ParametroRepository, EventoRepository}
    mapper/{AporteEntityMapper, SaldoEntityMapper, ParametroEntityMapper}       ← MapStruct
    adapter/{AportePostgresqlAdapter, SaldoPostgresqlAdapter, ParametroPostgresqlAdapter, EventoPostgresqlAdapter}
  entrypoint/
    rest/{AporteRestController, ConfiguracionRestController, ApiExceptionHandler}
    dto/request, dto/response
    mapper/{AporteRestMapper, ParametroRestMapper}                              ← MapStruct
    constant/ApiMessages
```

## 3. Convenciones aplicadas (reglas de arquitectura)

- **El dominio no importa Spring.** Los modelos usan solo Lombok (`@Data @Builder`); la
  lógica de negocio vive en los `*UseCase`, que son `@RequiredArgsConstructor` **sin**
  `@Component`.
- **Los casos de uso se cablean a mano** en `BeanConfiguration` (un `@Bean` por use case).
  Esto los mantiene testeables sin contexto de Spring.
- **Los `Handler` son la fachada transaccional** (`application/handler/adapter`,
  `@Service`), única capa que conoce transacciones.
- **Los `OutputPort` (gateways) los implementan adaptadores JPA** en
  `drivenadapter/postgresql/adapter`, usando **MapStruct** entre `*Entity` y el modelo.
- **Los controllers nunca ven el dominio directo.** Flujo:
  `Request DTO → RestMapper.toDomain() → Handler → RestMapper.toResponse() → Response DTO`.
- **El esquema vive en Flyway**, no en `@Entity`; `spring.jpa.hibernate.ddl-auto=validate`
  hace fallar el arranque si hay drift.

## 4. Flujo de una petición (registrar aporte)

```
POST /api/aportes
  → AporteRestController.registrar(RegistrarAporteRequest)      [entrypoint]
  → AporteRestMapper.toDomain(request)  →  Aporte (dominio)
  → AporteHandler.registrar(aporte)                            [application: tx + reintento]
      → AporteUseCase.registrar(aporte)                        [dominio puro: reglas]
          → AporteOutputPort / SaldoOutputPort / ParametroOutputPort / EventoOutputPort
              → *PostgresqlAdapter → MapStruct → JPA           [driven adapter]
  → AporteRestMapper.toResponse(aporte)  →  AporteResponse
```

## 5. Decisiones de diseño y tradeoffs

### 5.1 Dominio + lógica en el UseCase
Los modelos pasaron de una clase con comportamiento a `@Data @Builder`, y las reglas
(validación, decisión de estado, chequeo de tope/umbral, transiciones) se movieron al
`AporteUseCase`. **Tradeoff:** menos encapsulación OO, a cambio de fidelidad al patrón y
un dominio 100% testeable sin Spring.

### 5.2 Transacción y reintento en el Handler (no en el dominio)
El `AporteHandlerAdapter` orquesta lo que el dominio puro no debe conocer:
- **Lecturas** (`consultar`) → `@Transactional(readOnly = true)` (declarativo).
- **Escrituras** (`registrar`, `aprobar`, `rechazar`) → `TransactionTemplate` dentro de un
  loop de reintento, **cada intento en su propia transacción**.

**Por qué no solo `@Transactional`:** ante un conflicto de bloqueo optimista sobre
`saldo_mensual`, la transacción queda marcada *rollback-only*; no se puede reintentar
dentro de la misma tx. El reintento exige una tx fresca por intento y el loop **fuera** de
la transacción. `TransactionTemplate` lo expresa de forma explícita, sin dependencias
extra (Spring Retry) ni la trampa de auto-invocación de proxies.

La **carrera de idempotencia** (dos inserts con la misma clave a la vez) se resuelve en el
mismo loop: el perdedor recibe `DataIntegrityViolationException`, su tx hace rollback
(liberando la reserva de saldo) y el reintento encuentra el ganador en la verificación
previa del use case y lo devuelve.

### 5.3 Excepciones de dominio → HTTP
| Excepción de dominio | HTTP | Uso |
|---|---|---|
| `DomainValidationException` | **400** | monto no positivo, umbral > tope, datos obligatorios |
| `DomainNotFoundException` | **404** | aprobar/rechazar un id inexistente |
| `DomainBusinessRuleException` | **422** | supera el tope mensual, transición de estado inválida |
| `MethodArgumentNotValidException` (Bean Validation) | **400** | validación del request (errores por campo) |
| cualquier otra | **500** | error inesperado |

> El scaffold trae 4 excepciones estándar (400/404/409/401). Se añadió
> `DomainBusinessRuleException` (**422**) para preservar el contrato actual, donde tope y
> transición ya devolvían 422.

### 5.4 `ErrorResponse` con shape enriquecido
Se mantuvo el cuerpo de error `{ timestamp, status, error, mensaje, errores }` en vez del
`{ message }` mínimo del scaffold, porque el frontend, la colección Postman y los tests
dependen de `mensaje` / `errores` / `error`.

### 5.5 MapStruct
Reemplaza el mapeo manual (constructores/`from()`). Las entidades **no usan `@Builder`**
(usan `@NoArgsConstructor @AllArgsConstructor @Getter @Setter`) para que MapStruct mapee
por setters e incluya los campos heredados de `BaseEntity` (`created_at`/`updated_at`, que
se ignoran en `toEntity`). El `marcadaRevision` de la respuesta se deriva del estado en el
`AporteRestMapper` (expression).

`pom.xml`: `mapstruct` + `mapstruct-processor` + `lombok-mapstruct-binding` en los
`annotationProcessorPaths` (el binding evita conflictos Lombok ↔ MapStruct).

### 5.6 `BaseEntity` + migración `V3`
`BaseEntity` aporta `created_at` / `updated_at` gestionados por `@PrePersist`/`@PreUpdate`.
`V3__base_entity_timestamps.sql`:
- agrega `created_at` / `updated_at` a `aporte`, `saldo_mensual`, `parametro_aporte`;
- elimina `marcada_revision` (derivado del `estado`) y `creado_en` (reemplazado por
  `created_at`).

### 5.7 Unificación de mensajes de validación
Los mensajes del request (`RegistrarAporteRequest`) que también son invariantes de dominio
referencian las constantes de `AporteMessages` (son `static final String`, válidas como
`message` de Bean Validation). Así el DTO y el UseCase "hablan con una sola voz". `fecha`
queda con mensaje literal por ser un concern exclusivo del canal HTTP.

## 6. Contrato preservado (no cambió nada para el cliente)

- Endpoints idénticos: `POST /api/aportes`, `GET /api/aportes/consolidado`,
  `POST /api/aportes/{id}/aprobar`, `POST /api/aportes/{id}/rechazar`,
  `GET|PUT /api/configuracion/parametros`.
- DTOs de request/response con los mismos campos y códigos de estado (201/200/400/404/422).
- El frontend React y la colección Postman funcionan sin modificaciones.

## 7. Calidad añadida

| Herramienta | Detalle |
|---|---|
| **springdoc-openapi** | Swagger UI en `/swagger-ui.html` (`OpenApiConfiguration`) |
| **Actuator** | endpoints de salud/gestión |
| **logstash-logback-encoder** | disponible para logging estructurado |
| **JaCoCo** | reporte en `mvn verify` + **gate de cobertura** |

**Gate de cobertura** (property configurable):
```xml
<jacoco.coverage.minimum>0.85</jacoco.coverage.minimum>
```
Regla `BUNDLE → INSTRUCTION → COVEREDRATIO`. `mvn verify` falla si la cobertura baja del
mínimo. Override: `mvn verify -Djacoco.coverage.minimum=0.90`.

## 8. Pruebas

Migradas a la nueva estructura (**22 pruebas, cobertura ~93%**):
- **Dominio (Mockito, sin Spring):** `AporteUseCaseTest`, `ParametroUseCaseTest` —
  idempotencia, monto inválido, umbral → pendiente reservando cupo, tope → 422,
  aprobar/rechazar (libera reserva), validaciones de parámetros.
- **Integración web (`@SpringBootTest` + H2 + MockMvc):**
  `AporteRestControllerIntegrationTest`, `ConfiguracionRestControllerIntegrationTest` —
  contrato HTTP completo, idempotencia (mismo id, un registro), 400/422, reserva de cupo,
  ciclo pendiente → aprobado en el consolidado, configuración.
- **Smoke de contexto:** `RetoBApplicationTest`.

## 9. Verificación realizada

- `mvn test` → **22 verdes**.
- `mvn verify` → cobertura OK, *"All coverage checks have been met"*.
- **Arranque real contra PostgreSQL**: Flyway aplicó `V1 → V3` y `ddl-auto=validate` pasó
  sin drift.
- **Integración frontend ↔ backend** validada end-to-end (registro desde el formulario,
  carga de parámetros en la pantalla de Configuración vía el proxy de Vite).

## 10. Cómo ejecutar

```bash
# Base de datos (una vez): crear la BD vacía; Flyway crea las tablas al arrancar
#   CREATE DATABASE proteccion_reto;

cd reto-b/backend
mvn spring-boot:run        # API en :8082 · Swagger en /swagger-ui.html
mvn test                   # pruebas
mvn verify                 # pruebas + reporte + gate de cobertura JaCoCo

cd reto-b/frontend
npm install && npm run dev  # UI en :5173 (proxy /api → :8082)
```

## 11. Resumen

La refactorización reorganiza el backend a arquitectura hexagonal fiel al scaffold de
referencia (dominio puro, handlers transaccionales, driven adapter con MapStruct, entrypoint
REST), suma calidad (Swagger, Actuator, JaCoCo con gate) y **conserva intacto el contrato**,
por lo que ni el frontend ni las pruebas de API requirieron cambios. Las desviaciones
respecto del scaffold (excepción 422, `ErrorResponse` enriquecido, tx/reintento en el
handler) fueron decisiones conscientes para no romper el comportamiento existente.
