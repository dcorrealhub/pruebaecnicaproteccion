# Reto B — Notas del proceso (conversación con IA)

Registro de la sesión de trabajo asistida por IA (Claude Code) para implementar el **Reto B**: los prompts, las preguntas de decisión y sus respuestas, las correcciones pedidas en cada revisión y lo que se hizo en cada paso.

- **Mensajes del usuario:** se transcriben literalmente.
- **Respuestas de la IA:** se resumen (el código completo está en el repositorio).
- **Decisiones finales:** están consolidadas en [DECISIONES.md](DECISIONES.md).

---

## 1. Prompt inicial

> **Usuario:**
>
> # Contexto
>
> Vamos a implementar la funcionalidad **"Registro y consulta de aportes voluntarios"** en un proyecto con:
> - **Backend:** Spring Boot + PostgreSQL (arquitectura hexagonal).
> - **Frontend:** React (estructura básica: `api` y `components`).
>
> En el código hay comentarios `TODO` que marcan dónde debe implementarse la funcionalidad. Son el foco principal del trabajo.
>
> **Regla principal: consúltame cualquier decisión antes de tomarla.** No asumas nada que no esté definido aquí ni en el código existente. Si algo es ambiguo, pregúntame. No elijas tú una opción por defecto.
>
> # Fase 1: Análisis (antes de proponer cualquier cambio)
>
> Analiza el backend y el frontend y dame un resumen de:
>
> **Backend**
> - Cómo está aplicada la arquitectura hexagonal: paquetes y capas (dominio, aplicación/casos de uso, puertos de entrada y salida, adaptadores).
> - Convenciones de nombres de clases, interfaces, DTOs, mappers y excepciones.
> - Dónde y cómo se valida hoy: anotaciones de Bean Validation, validaciones en el dominio o en los casos de uso.
> - Manejo de errores: `@ControllerAdvice`, excepciones de dominio y formato de la respuesta de error.
> - Persistencia: entidades JPA frente a modelos de dominio; repositorios; migraciones (Flyway, Liquibase u otra); manejo de transacciones.
> - Configuración: cómo se definen hoy los parámetros configurables (`application.yml`, `@ConfigurationProperties`, base de datos).
> - Pruebas existentes: frameworks (JUnit 5, Mockito, Testcontainers, etc.), estructura y estilo.
>
> **Frontend**
> - Estructura de `api` y `components`.
> - Cómo se hacen las llamadas HTTP (fetch, axios u otra).
> - Cómo se maneja el estado.
> - Cómo se muestran los errores.
>
> **TODOs**
> - Lista de todos los `TODO` encontrados, con su archivo, su línea y qué parece pedir cada uno.
>
> **Entrega el análisis y espera mi confirmación antes de continuar.**
>
> # Fase 2: Funcionalidad a implementar
>
> **1. Registrar un aporte.** Un afiliado (identificado por un **id sintético**) registra un aporte a un fondo voluntario con monto, fecha y canal de origen. **La operación debe ser idempotente.**
>
> Reglas de negocio:
> 1. El monto debe ser **positivo**.
> 2. Existe un **tope mensual por afiliado**. Este tope es un parámetro configurable.
> 3. Un aporte que supere un **umbral definido** debe quedar **marcado para revisión posterior**. No se rechaza.
> 4. Los aportes que violen las reglas se **rechazan con un mensaje claro**.
>
> **2. Consultar el consolidado.** Dado un afiliado y un periodo, se devuelve el **total** y el **detalle** de los aportes.
>
> **3. Frontend (React).** Un formulario para registrar un aporte y una tabla con el consolidado. No necesita ser bonito, pero debe ser correcto y razonable. Debe mostrar los errores de negocio que devuelve el backend.
>
> # Decisiones que DEBES consultarme
>
> Antes de diseñar, plantéame estas preguntas, cada una con opciones y su trade-off:
> - **Idempotencia:** mecanismo (header `Idempotency-Key`, clave de negocio compuesta o id generado por el cliente) y comportamiento ante un reintento (mismo resultado o conflicto).
> - **Tope mensual:** qué es "mes" (calendario o ventana móvil); por qué fecha se cuenta; si los aportes marcados cuentan; si un aporte que rebasa el tope se rechaza completo o se acepta parcialmente; cómo evitar que dos aportes simultáneos superen el tope (bloqueo pesimista, optimista o restricción en BD).
> - **Umbral de revisión:** dónde se configura; si es por aporte individual; cómo se representa la marca (booleano, estado o enum).
> - **Datos del aporte:** canal (enum cerrado o texto libre); monto (`BigDecimal` / `NUMERIC(p,s)`, precisión y moneda); fecha (futuras o pasadas, zona horaria).
> - **Consulta del consolidado:** formato del periodo y si se valida (por ejemplo, rango máximo).
> - **Diseño técnico:** endpoints, verbos y códigos de respuesta (400, 409, 422); formato de la respuesta de error, si no existe ya uno.
>
> # Restricciones de implementación
> - Código limpio, siguiendo **exactamente** las convenciones y la arquitectura existentes. No introducir patrones, librerías ni capas nuevas sin consultar.
> - Reglas de negocio en el **dominio o en los casos de uso**, no en controladores ni adaptadores.
> - Validaciones de formato en la entrada (DTO / Bean Validation); reglas de negocio en el dominio.
> - Persistencia correcta: migración versionada, tipos adecuados, índices para la consulta por afiliado y fecha, restricción única que soporte la idempotencia, transacciones bien delimitadas.
> - Tope mensual y umbral configurables, no hardcodeados.
>
> # Pruebas
> Pocas pruebas, imprescindibles y bien hechas, siguiendo estrictamente el patrón **AAA (Arrange, Act, Assert)**, con nombres descriptivos. Proponer la lista antes de escribirlas. Como mínimo: registro exitoso; rechazo por monto no positivo; rechazo por superar el tope; aporte marcado para revisión; idempotencia; consolidado excluyendo aportes fuera de rango. Indicar si cada una es unitaria o de integración, alineado con lo que ya exista.
>
> # Formato del plan esperado
> 1. Resumen del análisis (Fase 1). 2. Preguntas de decisión, con opciones y recomendación. 3. Tras las respuestas, el plan por pasos (archivos por capa y qué `TODO` resuelve cada paso). 4. Riesgos o dudas abiertas.
>
> No escribas código hasta que apruebe el plan.

---

## 2. Fase 1: análisis

**IA:** leyó todo `reto-b` y entregó el análisis:
- **Hexagonal:** `domain/{model, port/in, port/out}`, `application/usecase/*UseCaseImpl` (`@Service`), `infrastructure/{web, web/dto, persistence/{adapter, entity, repository}}`. Los puertos viven en `domain`.
- **Convenciones:** `XUseCase` / `XUseCaseImpl` con `Command` y `Query` como records anidados; `XRepositoryPort`; `Jpa*Adapter`; `SpringData*Repository`; DTOs como records con `static from(...)`; sin mappers ni excepciones propias.
- **Validación:** solo Bean Validation en `RegistrarAporteRequest`; nada en el dominio ni en el GET.
- **Errores:** sin `@ControllerAdvice` ni formato; un `IllegalArgumentException` terminaba en 500.
- **Persistencia:** Flyway V1 con `aporte` (UNIQUE de la clave e índice afiliado+periodo), `saldo_mensual` (`version`) y `evento_aporte` (sin entidad ni puerto); `ddl-auto=validate`; sin transacciones.
- **Configuración:** `@Value` con valores por defecto en campos del caso de uso.
- **Pruebas:** solo `contextLoads` sobre H2.
- **Frontend:** `fetch` sin implementar; `useState` local; errores con `err.message` en un `<p>` rojo.
- **TODOs:** los dos casos de uso, los dos adaptadores JPA, `aportesApi.js` y los dos componentes.
- **Huecos detectados:** el request y el command no tenían `fecha`; el front generaba una clave nueva en cada envío; el evento no tenía puerto; había una carrera en `inicializar` del saldo; el controller respondía siempre 201; `canal` era texto libre; no existía `mvnw`.

> **Usuario:** (marcó "Hay correcciones")
>
> Esta todo bien, solo que corriges lo del 201, para que no devuelva siempre 201 aunque ya se haya creado por el tema de la clave de idempotencia

---

## 3. Preguntas de decisión

### Ronda 1: idempotencia y evento

| Pregunta | Respuesta del usuario |
|---|---|
| Mecanismo de idempotencia | `idempotenciaKey` en el body (recomendado) |
| Respuesta ante un reintento | *"incluso podrías manejar cuando es 422 Unprocessable Entity, es decir, se reutiliza la misma clave pero el contenido es diferente"* → **201** creado · **200** mismo contenido · **422** contenido distinto |
| Clave en el front | Una por intento de formulario (recomendado) |
| Evento `evento_aporte` | Persistir en la tabla (recomendado) |

### Ronda 2: tope mensual

| Pregunta | Respuesta |
|---|---|
| Qué es "mes" | Mes calendario por fecha del aporte (recomendado) |
| ¿Los marcados cuentan? | Sí cuentan (recomendado) |
| Rebase del tope | Rechazo completo (recomendado) |
| Concurrencia | Optimista con `@Version` (recomendado) |

### Ronda 3: umbral y datos

| Pregunta | Respuesta |
|---|---|
| Umbral | `properties`, por aporte, `monto > umbral` (recomendado) |
| Marca | Booleano `marcadaRevision` (recomendado) |
| Canal | Enum APP_MOVIL/WEB/SUCURSAL con CHECK (recomendado) |
| Monto | `BigDecimal` `NUMERIC(15,2)`, COP (recomendado) |

### Ronda 4: fecha, consolidado y errores

| Pregunta | Respuesta |
|---|---|
| Fecha | La envía el cliente; no futura (recomendado) |
| Periodo del consolidado | YYYY-MM desde/hasta, máximo 12 meses (recomendado) |
| Formato de error | `ProblemDetail` RFC 7807 (recomendado) |
| Excepciones | De dominio propias (recomendado) |

### Ronda 5: diseño interno

| Pregunta | Respuesta |
|---|---|
| Dónde viven las reglas | **En el caso de uso** (no se eligió la opción recomendada, que era el dominio) |
| Inyección de la configuración | **`@Value` por constructor** (no `@ConfigurationProperties`) |
| Cómo distinguir 201 de 200 | *"No me convencen ninguna de las dos opciones, dame otras alternativas que sean mas clean y que sigan la misma idea del proyecto."* |
| Base de datos de las pruebas | H2 como el proyecto (después se cambió, ver sección 4) |

**IA:** propuso tres alternativas: una `sealed interface` anidada en el puerto, un record con enum de resultado o una excepción de dominio para el reintento.

> **Usuario:** eligió **sealed interface anidada** (`RegistroAporte permits Creado, Repetido`, con un `switch` exhaustivo en el controller).

---

## 4. Revisión del plan

**IA:** escribió el plan y pidió aprobación.

> **Usuario:** El manejo de concurrencia explicame lo de @Version, que sentido tiene eso. Otra cosa, el manejo de errores se sigue el estándar del proyecto?

**IA:** explicó con un ejemplo la "actualización perdida" (dos transacciones que leen el mismo saldo y superan el tope) y cómo `@Version` convierte el `UPDATE` en `... WHERE version = N`: la segunda transacción afecta 0 filas, recibe `OptimisticLockException`, se revierte entera y el cliente reintenta de forma segura con la misma clave. Aclaró que el proyecto **no tenía** estándar de errores y que `ProblemDetail` más excepciones de dominio era algo nuevo, pero alineado con la estructura de paquetes existente.

> **Usuario:** Pero entonces el exception ya maneja nativamente el 409?

**IA:** no. Sin un manejador, Spring devuelve 500. La cadena es: el adaptador captura la excepción de JPA → lanza `ConcurrenciaConflictoException` (dominio) → `@RestControllerAdvice` → 409.

> **Usuario:** Okay dejemos, eso ahí me parece interesante para agregar un manejo de errores mas propio, adicional a lo que se exige dentro del alcance.
>
> Por otro lado, vamos a generar un docker y un docker compose para poder manejar de una manera fácil las pruebas junto con la base de datos, evitemos esos test en h2.
>
> Ultimas cosas, lo primero genera un MD markdown con todos los trade-offs o decisiones tomadas, y ejecutemos el plan paso por paso y al final de cada paso me das el nombre de un commit. Así vamos pasando punto por punto analizándolo bien lo que generas.
>
> Hay que tener en cuento otro aspecto dentro de ese plan y es que hubo una corrección, "Negocio nos avisa que los aportes por sucursal tienen mayor riesgo. A partir de ahora, un aporte por canal SUCURSAL se marca para revisión si supera 3.000.000. Los demás canales siguen con 5.000.000."

**IA:** hizo cuatro preguntas más:

| Pregunta | Respuesta |
|---|---|
| Cómo ejecutar las pruebas contra PostgreSQL | Servicio de tests en el compose (recomendado) |
| Qué incluye el compose | *"Me gustaria que estuviese, postgres, backend, frontend y tests"* |
| Umbral por canal | Mapa por canal + valor por defecto (recomendado) |
| Documento de decisiones | `reto-b/DECISIONES.md` (recomendado) |

> **Usuario** (al ver el plan): El documento de decisiones debe ser lo ultimo, porque puede que a medida que vayamos revisándolo vayan surgiendo cambios.

**IA:** movió el documento al paso 8. **Plan aprobado.**

---

## 5. Ejecución paso por paso

### Paso 1: dominio
**IA:** creó `Canal`, `TipoEventoAporte`, `EventoAporte`, las excepciones de dominio (`AporteRechazadoException`, `IdempotenciaConflictoException`, `ConcurrenciaConflictoException`, `ConsultaInvalidaException`) y `EventoAporteRepositoryPort`. Agregó `fecha` y `Canal` al command, y el resultado `sealed RegistroAporte`.
Commit: `feat(dominio): canal, evento, excepciones y contrato de registro idempotente`

> **Usuario:** Vamos a corregir algunas cosas:
> 1. Lo del enum dejalo en una carpeta enums, no lo dejes al nivel de model.
> 2. Porque lo del evento tiene OffsetDateTime
> 3. Como funciona lo del sealed y pues el elegir a partir de cual codigo se devolvera

**IA:**
1. Movió los enums a `domain/model/enums`.
2. Explicó que `ocurrido_en` es `TIMESTAMPTZ` (un instante) y que el proyecto ya usa `OffsetDateTime` para `creadoEn`; `LocalDateTime` perdería la zona horaria.
3. Explicó que `sealed` limita las implementaciones a `Creado` y `Repetido`. El caso de uso decide el resultado de negocio y el controller lo traduce a HTTP con un `switch` sin `default`: un tercer resultado no compilaría hasta mapearlo.

Preguntó quién asigna `ocurridoEn`.

> **Usuario:** prosigamos con el siguiente paso → `ocurridoEn`: **la entidad con `@PrePersist`** · enums: **se quedan en `domain/model/enums`**.

### Paso 2: casos de uso
**IA:** implementó `RegistrarAporteUseCaseImpl` (idempotencia con comparación de contenido, monto, fecha no futura con `Clock`, tope sobre `saldo_mensual`, umbral por canal, persistencia de saldo, aporte y evento en una sola transacción) y `ConsultarAportesUseCaseImpl` (validación del rango y total con `BigDecimal::add`). Ambos con constructor explícito y `@Value`.
Commit: `feat(aplicacion): casos de uso de registro idempotente y consolidado de aportes`

> **Usuario:** entonces que debo agregar para que cumpla este nuevo requisito: "Negocio nos avisa que los aportes por sucursal tienen mayor riesgo. A partir de ahora, un aporte por canal SUCURSAL se marca para revisión si supera 3.000.000. Los demás canales siguen con 5.000.000."

**IA:** el código ya lo soporta (`umbralRevisionPorCanal.getOrDefault(canal, default)`); faltaban solo la propiedad `aporte.umbral-revision.por-canal={SUCURSAL:'3000000'}` y la prueba parametrizada.

> *(El usuario cambió la clave del umbral por defecto a `aporte.umbral-revision` y agregó las propiedades al `application.properties`.)*
>
> **Usuario:** prosigamos con el siguiente paso del plan

### Paso 3: persistencia y migración V2
**IA:** implementó los adaptadores JPA con `saveAndFlush` y la traducción de `OptimisticLockingFailureException` y de las violaciones `uq_aporte_idempotencia` / `uq_saldo_afiliado_mes` a `ConcurrenciaConflictoException`. Agregó `EventoAporteEntity`, el repositorio y el adaptador del evento, la consulta ordenada por fecha y `V2__restricciones_aporte.sql` (CHECK de canal, `monto > 0` y `total >= 0`).

> **Usuario:** lo del porte.consulta-rango-maximo-meses, de que paso era

**IA:** se usa en el paso 2 (caso de uso de consulta) y se define en el paso 4 (`application.properties`).

> **Usuario:** entonces a parte de eso que debo agregar al properties proveniente del paso 2

**IA:** solo `aporte.umbral-revision.por-canal` y `aporte.consulta-rango-maximo-meses`. `aporte.zona-horaria` corresponde al paso 4 (`ClockConfig`).

> **Usuario:** separa los mapper dentro de los jpa adaptes a afuera a una carpeta llamada mappers

**IA:** creó `infrastructure/persistence/mappers/` (`AporteMapper`, `SaldoMensualMapper`, `EventoAporteMapper`) como clases `final` con métodos estáticos.
Commit: `feat(persistencia): adaptadores JPA con mappers, evento de aporte y restricciones V2`

> **Usuario:** prosigue con el siguiente punto del plan

### Paso 4: web y configuración
**IA:**
- **Request:** `fecha`, `Canal`, `@Digits` y `@Size`.
- **Controller:** `switch` que responde 201 o 200, y `@Pattern` YYYY-MM en el GET.
- **`GlobalExceptionHandler`:** `ProblemDetail` con `codigo` y `errores`; 400, 422, 409 y 500.
- **Configuración:** `ClockConfig` y `aporte.zona-horaria=America/Bogota`.
- **Verificación:** arrancó la app contra un PostgreSQL temporal en Docker y probó con curl todos los casos (201, 200, 422, 400, consolidado, umbral SUCURSAL).

Commit: `feat(web): respuesta idempotente 201/200 y manejo de errores con ProblemDetail`

> **Usuario:** Termina de ejcutar todo los pasos restantes

### Paso 5: Docker
**IA:** creó el Dockerfile multi-stage del backend (etapas `test`, `build` y `runtime`), el Dockerfile del frontend (Vite build + nginx con proxy `/api`) y `reto-b/docker-compose.yml` con `postgres`, `postgres-test` (`tmpfs`, puerto 5433), `backend`, `frontend` y `backend-tests` (perfil `tests`).
Commit: `build: docker compose con postgres, backend, frontend y servicio de pruebas`

### Paso 6: pruebas sin H2
**IA:** eliminó H2 del `pom.xml` y agregó `application-test.properties` (PostgreSQL en 5433, Flyway real). Escribió 6 pruebas unitarias (una de ellas parametrizada con 4 casos para el umbral por canal) y 2 de integración (consolidado e idempotencia de extremo a extremo), en AAA estricto con `catchThrowable` en *Act*.
**Resultado:** `docker compose run --rm backend-tests` → **12 pruebas en verde**.
Commit: `test: casos de uso y persistencia contra PostgreSQL sin H2`

### Paso 7: frontend
**IA:**
- **`aportesApi.js`:** `fetch`, lectura del `ProblemDetail` (incluidos los errores por campo) y `{ aporte, repetido }`.
- **`RegistrarAporte.jsx`:** campo fecha, clave por intento y monto enviado como texto.
- **Consolidado:** formato de moneda.
- **Verificación:** con `docker compose up -d --build`, en el navegador: un aporte SUCURSAL de 3.500.000 quedó marcado para revisión, el exceso de tope mostró el mensaje del backend y el consolidado mostró el total y la tabla.

Commit: `feat(frontend): formulario de registro y consolidado de aportes`

### Paso 8: documento de decisiones
**IA:** escribió `DECISIONES.md` con todas las decisiones finales, alternativas y trade-offs, incluidos los ajustes surgidos en la revisión, el cambio de negocio de SUCURSAL, el contrato de errores, Docker, las pruebas y los riesgos.
Commit: `docs: registrar decisiones y trade-offs del reto B`

> **Usuario:** Generame otro markdown donde muestres toda nuestra conversacion, del reto a ahi

**IA:** generó este archivo (`NOTAS_PROCESO.md`, el nombre que pide el README para conservar los prompts y notas).
Commit: `docs: notas del proceso asistido por IA del reto B`

---

## 6. Observaciones del proceso

- La IA no tomó decisiones sin consultar. Las únicas elecciones de implementación menores (nombres de códigos de error, `ViolacionRestriccion`, mappers estáticos, nginx para el front) se informaron al cerrar cada paso para que el usuario las revisara.
- El usuario se apartó de la recomendación en tres puntos: reglas en el caso de uso (en lugar del dominio), `@Value` por constructor (en lugar de `@ConfigurationProperties`) y la forma de distinguir 201 de 200 (pidió alternativas y eligió `sealed`).
- Cambios del usuario durante la ejecución: enums en una subcarpeta, mappers fuera de los adaptadores, `ocurridoEn` por `@PrePersist`, PostgreSQL real en lugar de H2, documento de decisiones al final y la clave `aporte.umbral-revision` editada a mano.
- Cada paso se verificó compilando. Desde el paso 4 también se verificó en ejecución real (curl, pruebas en Docker y navegador).
