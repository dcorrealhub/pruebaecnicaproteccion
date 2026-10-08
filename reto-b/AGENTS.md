# AGENTS.md — Reto B · Aportes Voluntarios (CIS Protección S.A.)

Contexto fijo para cualquier agente/IA que trabaje en `reto-b/`. Léelo antes de generar código.
No repitas estas reglas en cada prompt: asúmelas siempre.

## 1. Contexto

- Dominio: módulo de **aportes voluntarios** de un afiliado (id **sintético**, p.ej. `AF-001`) a un fondo.
- Organización: CIS — Protección S.A. Entorno **regulado por la SFC**.
- No negociables: **corrección, seguridad y trazabilidad**.
- **Datos SINTÉTICOS únicamente. Nunca PII real** (nombres, cédulas, cuentas, saldos reales).

## 2. Stack (versiones reales del scaffold)

- Backend: **Spring Boot 3.4.1**, **Java 21**, Maven. Lombok (solo infraestructura/DTO, no dominio).
- Persistencia: **PostgreSQL** + **Flyway** (`src/main/resources/db/migration`). H2 solo en tests.
- Frontend: **React 18.3** + **Vite 6**, **fetch nativo** (sin axios). Proxy `/api` → `http://localhost:8082`.
- Config clave: `aporte.tope-mensual`, `aporte.umbral-revision`, `server.port=8082`, `ddl-auto=validate`.

## 3. Arquitectura (hexagonal / Clean + SOLID + CQRS)

- Regla de dependencias: **el dominio no conoce la infraestructura**. Las flechas apuntan hacia adentro.
  - `domain/model`: POJOs puros, sin `@Entity`, `@Service`, Lombok ni Spring.
  - `domain/port/in`: casos de uso (comandos/consultas). `domain/port/out`: puertos de repositorio.
  - `application/usecase`: implementa los puertos de entrada, orquesta puertos de salida.
  - `infrastructure/{persistence,web}`: adaptadores (JPA, REST). Mapea entity↔dominio en el adaptador.
- **CQRS**: `RegistrarAporte...` = comando (muta estado); `ConsultarAportes...` = consulta (`readOnly`).
  No mezclar lectura y escritura en el mismo caso de uso.
- SOLID: una responsabilidad por clase; depender de puertos (interfaces), no de implementaciones.

## 4. Dinero (crítico)

- **Siempre `BigDecimal`**, escala **2**, con `RoundingMode` **explícito** (`setScale(2, RoundingMode.HALF_EVEN)`).
- Columnas monetarias: **`NUMERIC(19,2)`**.
- Comparar montos con **`compareTo`**, nunca `==` ni `equals`.
- **Prohibido `double`/`float`** para montos en cualquier capa (incluido el borde del frontend al enviar).

## 5. Reglas de negocio (viven en el dominio)

- `monto > 0` (invariante de dominio, no solo del borde).
- **Tope mensual** por afiliado y mes, **configurable** (`aporte.tope-mensual`). Rechazar si el acumulado del mes + monto supera el tope.
- **Umbral de revisión** (`aporte.umbral-revision`): si el monto lo supera, el aporte se persiste **marcado para revisión** (`marcadaRevision = true`), no se rechaza.
- Rechazos con **mensaje claro** y accionable; sin filtrar detalles internos.

## 6. Idempotencia

- Clave de idempotencia provista por el cliente (`idempotenciaKey`, p.ej. `crypto.randomUUID()`).
- **UNIQUE en BD** sobre `idempotencia_key` (ya existe en `V1`).
- Ante reintento con la misma clave: **devolver la respuesta original** (mismo recurso), sin duplicar aporte ni re-sumar saldo. Resolver la carrera apoyándose en la restricción de unicidad, no solo con un `findBy` previo.

## 7. Validación (dos capas)

- Borde (DTO web) con Bean Validation: `@Valid`, `@NotBlank`, `@NotNull`, `@Positive`/`@DecimalMin`, `@Digits(integer=17, fraction=2)`, **enum para `canal`** (`APP_MOVIL`, `WEB`, `SUCURSAL`).
- Dominio: **revalidar invariantes** (monto > 0, tope, periodo) aunque el borde ya haya validado. El dominio no confía en el borde.

## 8. Persistencia

- Esquema **solo por Flyway**. **Prohibido `ddl-auto=create-drop`/`update`** en main (`validate`).
- Constraints en BD: `NOT NULL`, **`CHECK (monto > 0)`**, UNIQUE `(afiliado_id, mes)` en saldo, UNIQUE idempotencia.
- `@Transactional` en los casos de uso de escritura; `@Transactional(readOnly = true)` en consultas.
- Concurrencia del saldo mensual: usar **bloqueo optimista (`@Version`, ya presente)** o pesimista, o un **UPDATE atómico condicionado al tope**. No leer-modificar-escribir sin protección.
- Nuevas migraciones: `V2__...`, `V3__...` incrementales. No editar `V1` ya aplicada.

## 9. Errores (API)

- `@RestControllerAdvice` centralizado. Contrato **ProblemDetail (RFC 7807)**.
- Códigos: **201 + header `Location`** al crear; `400` validación de formato; `404` recurso no hallado; `409` conflicto (idempotencia/concurrencia); `422` violación de regla de negocio (tope, monto).
- **Sin eco del input** del usuario ni fuga de stacktrace/detalles internos en la respuesta.

## 10. Observabilidad

- Logs **sin datos sensibles**: no loguear `monto` ni `afiliadoId` en claro.
- Propagar un **id de correlación** por request (header, p.ej. `X-Correlation-Id`) e incluirlo en los logs.

## 11. Tests (mínimos imprescindibles)

- **Dominio**: invariantes (monto, tope, umbral de revisión) — unitarios puros, sin Spring.
- **Caso de uso**: idempotencia (reintento no duplica), rechazo por tope, marcado por umbral — con puertos mockeados.
- **Persistencia/integración**: UNIQUE de idempotencia, consolidado por rango de periodo, concurrencia sobre el saldo.
- Casos de **límite del tope** (justo igual, justo por encima), **concurrencia** e **idempotencia** son obligatorios.

## 12. Anti-patrones PROHIBIDOS (hallazgos del Reto A — no repetir)

1. `double`/`float` para dinero.
2. Comparar el tope con `==` (usar `compareTo`).
3. SQL por **concatenación** de strings (usar parámetros / métodos derivados / `@Query` parametrizado).
4. Endpoints **sin autenticación** (dejar el hook de seguridad previsto, no abrir todo).
5. **Exponer la entidad JPA** en la API (siempre DTO en el borde).
6. Registro **no idempotente**.
7. Saldo **no segmentado por mes**.
8. Falta de `@Transactional` en operaciones de escritura.

---

## 13. Desviaciones del scaffold frente a estas reglas (resolver, no ignorar)

El scaffold entregado ya contradice algunas reglas de arriba. Documentado aquí para corregirlo de forma consciente al implementar (no en silencio):

- **Dinero `NUMERIC(15,2)`** en `V1__init.sql` y en las entidades (`precision = 15`), pero la regla exige **`NUMERIC(19,2)`**. Alinear vía nueva migración `V2` + `precision = 19` en las entidades.
- **Falta `CHECK (monto > 0)`** en la tabla `aporte`. Agregar en migración.
- **No existe `@RestControllerAdvice` ni ProblemDetail**: hoy un error de negocio o un `UnsupportedOperationException` saldría como `500` genérico. Crear el advice con el contrato RFC 7807 y los códigos 400/404/409/422.
- **201 sin `Location`**: el controller usa `@ResponseStatus(CREATED)` pero no setea el header `Location`. Devolver `ResponseEntity` con `Location`.
- **`canal` es `String` libre** en DTO, command, dominio y columna. La regla pide **enum**. Introducir enum y validarlo en el borde.
- **El command no incluye `fecha`** (`RegistrarAporteCommand` solo trae afiliado, monto, canal, idempotencia), pero el brief pide registrar **monto, fecha y canal**. Decidir: aceptar `fecha` del cliente (validada) o derivarla en servidor; dejarlo explícito. El `periodo` (YYYY-MM) debe derivarse de esa fecha.
- **Sin bloqueo pesimista disponible**: `SpringDataSaldoRepository` solo tiene `findByAfiliadoIdAndMes` sin `@Lock`. Si se opta por pesimista o UPDATE atómico, añadir la query correspondiente; si se usa optimista, manejar `OptimisticLockException` → `409`.
- **Tabla `evento_aporte` sin puerto ni repositorio**: existe en `V1` (trazabilidad: `APORTE_REGISTRADO`) pero no hay `EventoRepositoryPort`. Si se usa para trazabilidad, crear el puerto/adaptador; si no, decidir conscientemente no poblarla.
- **Sin autenticación**: no hay capa de seguridad. En un entorno SFC los endpoints no deberían quedar abiertos; dejar el punto de extensión previsto y documentar el gap.
