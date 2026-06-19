# Paso a paso de la realización de la Prueba Técnica AI-First · CIS Protección S.A.

## DEV: SANTIAGO ROLDÁN MUÑOZ

## Reto A — Auditoría de código

Para comenzar con la ejecución del **Reto A** (Auditoría de código) se comienza con el reconocimiento de la arquitectura y cada uno de los archivos que esta contiene. Luego de revisar uno a uno los archivos, como desarrollador se encuentran los errores principales, visibles fácilmente, encontrando los siguientes y planteándoles su respectiva solución (en palabras):

 1. Inyección SQL en endpoint de consulta (CRÍTICA)

 2. Ausencia total de autenticación y autorización (CRÍTICA)

 3. Dinero modelado con `double` (CRÍTICA)

 4. Control de tope mensual inoperante (`==` en vez de `>`) (CRÍTICA)

 5. Violación de capas y arquitectura (ALTA)

 6. Entidad JPA expuesta como contrato de API, sin validación de entrada (ALTA)

 7. Trazabilidad de auditoría insuficiente en `EventoAporte` (ALTA)

Puedes revisar la información completa en el archivo HALLAZGOS.md

---

### Segunda revisión con apoyo de Claude

Una vez como desarrollador se termina de revisar a profundidad los archivos y encontrando la mayoría de errores posibles (visibles con facilidad), se procede a una segunda revisión con ayuda de Claude, y exceptuando los errores ya descubiertos, asegurando así que queden cubiertos todos los errores del código.

El prompt utilizado en Claude fue el siguiente:

> "HALLAZGOS MERGE REQUEST.txt" (se le adjunta el TXT con los hallazgos)
>
> "Actúa como un revisor senior de Merge Requests en una fintech regulada. Tu stack es Spring Boot, React, PostgreSQL y Kubernetes en AWS, con GitLab CI/CD y controles DevSecOps activos. Tienes experiencia auditando código que procesa dinero real en entornos sujetos a normativas (DIAN, UIAF, SFC o equivalentes).
>
> **Contexto del módulo a revisar:**
> El reto-a es un microservicio Spring Boot que registra aportes a un fondo de inversión voluntaria. Opera sobre un modelo de Clean Architecture (dominio, aplicación, infraestructura), aplica separación comando/consulta (CQS), y corre en producción junto a otros servicios de la célula.
>
> **Tu tarea:**
> Exceptuando los hallazgos especificados en el archivo TXT, revisa el código completo como si fuera un MR bloqueante. No busques cantidad de hallazgos, busca los que un revisor experimentado detendría antes de aprobar.
>
> **Dimensiones obligatorias de análisis:**
> Corrección numérica, uso de `BigDecimal` vs `double`/`float`, redondeo, escala, overflow.
> Concurrencia y consistencia, condiciones de carrera, transaccionalidad reactiva, niveles de aislamiento.
> Idempotencia, reintentos, duplicados, claves de idempotencia en la API.
> Seguridad, inyección, exposición de datos sensibles, autenticación/autorización, secretos en código.
> Límites arquitectónicos, violaciones de capas (dominio con dependencias de infra, lógica de negocio en controladores), SOLID, CQS.
> Operabilidad — manejo de errores, logging de auditoría, trazabilidad, health checks.
>
> **Formato de entrega**
> Por cada hallazgo:
> - Ubicación: clase/método/línea (o patrón si aplica a varios sitios)
> - Severidad: CRÍTICA / ALTA / MEDIA / BAJA con una línea que justifique el nivel en términos de impacto de negocio o riesgo regulatorio
> - Problema: qué está mal y por qué importa en este contexto financiero
> - Corrección: código o patrón concreto, no solo consejo genérico"

Se procede con la revisión de los errores encontrados por Claude, y documentando los siguientes:

 8. Condición de carrera y falta de atomicidad transaccional (CRÍTICA)

 9. No hay protección contra reintentos (falta de idempotencia) (ALTA)

 10. Sin manejo global de errores — códigos HTTP incorrectos (MEDIA)

 11. Ausencia real de separación CQRS (MEDIA)

 12. Se están guardando datos privados de clientes en los archivos de texto — logs expuestos (MEDIA)

 Extra (para producción): persistencia volátil — sin perfiles para producción

 Puedes revisar la información completa en el archivo HALLAZGOS.md
 
---

### Redacción del archivo HALLAZGOS.md

Finalmente con ayuda del mismo Claude se procede con la redacción del archivo `HALLAZGOS.md`, indicándole que agregue el código posible para la corrección de los errores de acuerdo al contexto del proyecto y lo especificado en los apartados de corrección, con el siguiente prompt:

> "Leé el archivo adjunto y generá un archivo HALLAZGOS.md en la carpeta reto-a con los hallazgos de revisión del MR.
> Estructura por hallazgo (respetá este orden):
>
> ## [NÚMERO]. [Título del hallazgo]
> **Riesgo:** Crítico | Alto | Medio | Bajo
> **Descripción**
> Explicá qué está mal y por qué representa un problema. Usá lenguaje claro, sin jerga innecesaria.
> **Solución**
> Explicá qué hay que cambiar y por qué esa es la forma correcta. Una o dos oraciones directas.
> **Ejemplo de corrección**
> [bloque de código con el antes y el después, o solo el después si el contexto es claro]
>
> Sección final — Veredicto. Cerrá el archivo con un bloque así:
>
> ---
> ## Veredicto del MR
> **Estado:** BLOQUEADO
> **Motivo:** [dos o tres oraciones explicando por qué este MR no puede aprobarse en su estado actual, mencionando los hallazgos críticos o altos que lo determinan]
>
> Criterios de redacción:
> - Escribí en español, en tono profesional pero directo
> - Evitá anglicismos y abreviaciones que no sean universales
> - Cada descripción debe poder entenderla un desarrollador junior o senior tenga o no experiencia en el dominio (preferiblemente mantener la redacción original)
> - No repitas información entre la descripción y la solución: la descripción diagnostica, la solución prescribe"

Una vez Claude ha redactado el archivo `HALLAZGOS.md` se revisa que quede completamente listo y con esto se da por finalizada la revisión de la Merge Request y como completado el **Reto A**.

---

## Reto B — Implementación del módulo de aportes voluntarios

Para el **Reto B** se hace un uso "contrario" al realizado en el primero. En este caso la IA es la que nos ayuda con la realización de todo el código, y nosotros hacemos la supervisión de que todo esté funcionando correctamente.

Como primer paso le entregamos a la IA un prompt completo con todo el contexto y entregables que debe generar. Pero antes de ejecutar directamente hacemos uso de su modelo Opus 4.8 para generar el plan completo de ejecución. Así, antes de realizar cualquier modificación directa en el código, nos aseguramos de que Claude ejecute lo que realmente requerimos (además de hacer algunas adaptaciones a dicho plan). El prompt entregado fue el siguiente:

> "@reto-b\ Actuá como un desarrollador senior especializado en Spring Boot con WebFlux, React y PostgreSQL. Tenés experiencia construyendo módulos financieros en entornos regulados (SFC, UIAF, DIAN o equivalentes), con énfasis en corrección numérica, idempotencia y trazabilidad.
>
> **Contexto**
> Vas a implementar el módulo de registro de aportes voluntarios para una plataforma de fondos de inversión. El módulo debe integrarse en una arquitectura limpia (Clean Architecture) con separación comando/consulta (CQS). Toda operación que mueva dinero debe ser auditable y trazable.
>
> **Funcionalidad a implementar**
>
> **1. Registro de aporte (POST /aportes)**
> Registra un aporte de un afiliado a un fondo voluntario. Campos requeridos: identificador sintético del afiliado, monto, fecha y canal de origen.
>
> Reglas de negocio:
> - El monto debe ser positivo y representarse con `BigDecimal` (nunca `double` ni `float`)
> - Existe un tope mensual por afiliado, configurable vía parámetro externo (no hardcodeado)
> - Un aporte que supere un umbral configurable debe quedar marcado para revisión posterior (`requiere_revision = true`)
> - Cualquier aporte que viole las reglas se rechaza con un mensaje de error claro y un código HTTP apropiado
>
> Idempotencia:
> - La operación debe ser idempotente. Usá una clave de idempotencia (por ejemplo, un UUID enviado por el cliente en el header `Idempotency-Key`). Si la misma clave ya fue procesada, retorná el resultado original sin reprocesar.
>
> **2. Consulta de consolidado (GET /aportes/{afiliadoId}/consolidado)**
> Retorna el total y el detalle de aportes de un afiliado en un rango de fechas (parámetros `desde` y `hasta`).
>
> **Capa de base de datos (PostgreSQL)**
> - Diseñá el esquema con los tipos correctos (`NUMERIC` para montos, nunca `FLOAT`)
> - Incluí índices para las consultas por afiliado y rango de fechas
> - Las operaciones de escritura deben ejecutarse dentro de transacciones explícitas
> - Incluí una tabla o columna de auditoría con `created_at`, `updated_at` y usuario o canal de origen
>
> **Vista React**
> Implementá dos componentes:
> - Formulario de registro: campos para afiliado, monto, fecha y canal. Debe validar en cliente antes de enviar, manejar el estado de carga y mostrar errores del servidor de forma legible.
> - Tabla de consolidado: muestra total y detalle para un periodo seleccionado. Incluí filtros de fecha y un indicador visual para aportes marcados como `requiere_revision`.
>
> Usá la skill de frontend-design para construir una interfaz coherente, accesible y con buen criterio UX. La interfaz debe poder usarla un operador sin capacitación técnica.
>
> **Estándares transversales**
> Aplicá estos criterios en toda la implementación:
> - Seguridad: no expongas IDs internos en la API; validá y sanitizá todas las entradas
> - Errores: usá un manejador global de excepciones con respuestas estructuradas (timestamp, código, mensaje)
> - Logging: registrá entradas, salidas y rechazos con nivel y contexto suficiente para auditoría
> - Configuración: todos los umbrales y topes van en propiedades externas, no en el código
> - Pruebas: incluí al menos un test unitario por regla de negocio y un test de integración para el endpoint de registro"

### Plan generado y ajustado

Seguidamente Claude nos devuelve el plan al cual le realizamos algunos ajustes, quedando de esta manera:

> **Plan: Implementación módulo de aportes voluntarios — reto-b**
>
> **Contexto**
>
> El scaffold de reto-b tiene la arquitectura hexagonal definida (domain models, ports, use case interfaces, JPA entities, Spring Data repos, DTOs y controller) pero todos los métodos de negocio retornan `UnsupportedOperationException`. El objetivo es implementar la lógica completa: reglas de negocio, persistencia, manejo global de errores, CORS, docker-compose y la interfaz React.
>
> No hay `docker-compose.yml` en el repositorio (el `application.properties` lo referencia pero no existe). Los `AporteRepositoryPort` y `SaldoRepositoryPort` ya existen — los adaptadores los implementan; se leerán en ejecución antes de tocarlos.
>
> **Decisiones de diseño**
>
> *Idempotencia vía header HTTP* — el controller lee `Idempotency-Key` del request header (`@RequestHeader`), no del body. El DTO `RegistrarAporteRequest` elimina `idempotenciaKey` y agrega `fecha`. Esto sigue el estándar HTTP (RFC 8941) y mantiene el payload semánticamente limpio.
>
> *Excepción de dominio tipada* — nueva clase `domain/exception/ReglaNegocioException` (extends `RuntimeException`). El `@ControllerAdvice` la mapea a 422 Unprocessable Entity con cuerpo estructurado `{ timestamp, codigo, mensaje }`. Las violaciones de `@Valid` van a 400 Bad Request. Los `OptimisticLockException` de Hibernate van a 409 Conflict.
>
> *Ruta del consolidado* — `GET /api/aportes/{afiliadoId}/consolidado?periodoDesde=YYYY-MM&periodoHasta=YYYY-MM` — sigue la especificación del prompt (path param para el afiliado, query params para el rango).
>
> *Concurrencia en saldo mensual* — `SaldoMensualEntity` ya tiene `@Version` (optimistic locking de Hibernate). La secuencia en el use case es: leer saldo → validar tope → crear aporte → actualizar saldo → guardar ambos en la misma transacción. Si hay conflicto concurrente, Hibernate lanza `OptimisticLockException` → 409 Conflict. El cliente reintenta con la misma `Idempotency-Key` sin duplicar datos.
>
> *Campo fecha obligatorio en el request* — la fecha del aporte viene del cliente (campo obligatorio en body). El período (YYYY-MM) se deriva en el use case con `fecha.format(DateTimeFormatter.ofPattern("yyyy-MM"))`.
>
> *Frontend: sin librería de UI externa* — se mantiene React + Vite sin dependencias nuevas. Se agrega `index.css` con variables CSS y estilos centralizados; se eliminan todos los `style={{}}` inline de los componentes. Indicador visual de revisión: badge naranja con ícono de advertencia.
>
> **Archivos a crear (nuevos)**
>
> | Archivo | Propósito |
> |---|---|
> | `docker-compose.yml` (raíz del proyecto) | PostgreSQL 16 en :5432, DB `proteccion_reto` |
> | `domain/exception/ReglaNegocioException.java` | Excepción tipada para violaciones de negocio |
> | `infrastructure/web/GlobalExceptionHandler.java` | `@ControllerAdvice` con respuestas `{timestamp, codigo, mensaje}` |
> | `infrastructure/config/CorsConfig.java` | CORS para `http://localhost:5173` |
> | `test/.../RegistrarAporteUseCaseImplTest.java` | Tests unitarios por regla de negocio (mocks, sin Spring) |
> | `test/.../AporteControllerIT.java` | Test de integración con H2 para el endpoint POST |
> | `src/index.css` | Estilos globales del frontend (variables, layout, formularios, tabla) |
>
> **Archivos a modificar**
>
> | Archivo | Cambio |
> |---|---|
> | `infrastructure/web/dto/RegistrarAporteRequest.java` | Agregar `fecha: LocalDate`, eliminar `idempotenciaKey` |
> | `domain/port/in/RegistrarAporteUseCase.java` | Agregar `fecha: LocalDate` al Command record |
> | `infrastructure/web/AporteController.java` | Leer `@RequestHeader("Idempotency-Key")`; cambiar ruta consolidado a `/{afiliadoId}/consolidado` |
> | `infrastructure/persistence/adapter/JpaAporteRepositoryAdapter.java` | Implementar los 3 métodos (mapeo Entity ↔ Domain) |
> | `infrastructure/persistence/adapter/JpaSaldoRepositoryAdapter.java` | Implementar los 3 métodos (mapeo Entity ↔ Domain) |
> | `application/usecase/RegistrarAporteUseCaseImpl.java` | Implementar lógica completa con `@Transactional` |
> | `application/usecase/ConsultarAportesUseCaseImpl.java` | Implementar con `@Transactional(readOnly = true)` |
> | `src/api/aportesApi.js` | Implementar `registrarAporte` y `consultarConsolidado` con fetch |
> | `src/components/RegistrarAporte.jsx` | Validaciones adicionales, indicador de revisión, estilos CSS |
> | `src/components/ConsolidadoAportes.jsx` | Badge visual para `marcadaRevision`, estilos CSS |
> | `src/main.jsx` | Importar `index.css` |
>
> **Lógica del caso de uso `RegistrarAporteUseCaseImpl`**
>
> ```
> @Transactional
> registrar(command):
>   1. buscar aporte por idempotenciaKey → si existe, retornar sin reprocesar
>   2. validar monto > 0 → ReglaNegocioException si no
>   3. calcular periodo = command.fecha().format("yyyy-MM")
>   4. buscar saldo mensual del afiliado para ese periodo (o inicializar en 0)
>   5. validar (saldo.total + monto) <= topeMensual → ReglaNegocioException si supera
>   6. marcadaRevision = (monto.compareTo(umbralRevision) > 0)
>   7. construir Aporte (sin id aún)
>   8. persistir aporte → obtener aporte con id generado
>   9. actualizar saldo: saldo.conTotal(saldo.total + monto)
>   10. guardar saldo actualizado
>   11. retornar aporte persistido
> ```
>
> **Respuesta estructurada de errores**
>
> ```json
> {
>   "timestamp": "2026-06-19T10:30:00Z",
>   "codigo": "TOPE_MENSUAL_EXCEDIDO",
>   "mensaje": "El monto solicitado supera el tope mensual configurado para este afiliado"
> }
> ```
>
> **Orden de implementación**
>
> 1. `docker-compose.yml`
> 2. `ReglaNegocioException`
> 3. Modificar `RegistrarAporteRequest` + `RegistrarAporteCommand` (agregar fecha, quitar idempotenciaKey del body)
> 4. Modificar `AporteController` (header, ruta consolidado)
> 5. `GlobalExceptionHandler`
> 6. `CorsConfig`
> 7. Implementar `JpaAporteRepositoryAdapter`
> 8. Implementar `JpaSaldoRepositoryAdapter`
> 9. Implementar `RegistrarAporteUseCaseImpl`
> 10. Implementar `ConsultarAportesUseCaseImpl`
> 11. Tests unitarios (`RegistrarAporteUseCaseImplTest`) — al menos 4 casos: monto inválido, tope excedido, marcado revisión, idempotencia
> 12. Test de integración (`AporteControllerIT`) — POST exitoso + POST duplicado
> 13. Frontend: `aportesApi.js`
> 14. Frontend: `index.css` + componentes (eliminar estilos inline, agregar validaciones y badge)
>
> **Verificación end-to-end**
>
> 1. `docker compose up -d` desde la raíz del repositorio
> 2. `./mvnw test` en `reto-b/backend` — todos los tests deben pasar en verde
> 3. `./mvnw spring-boot:run` — backend disponible en `http://localhost:8082`
> 4. `npm run dev` en `reto-b/frontend` — frontend en `http://localhost:5173`
> 5. Happy path: registrar aporte desde la UI → confirmar respuesta 201 Created con `marcadaRevision`
> 6. Idempotencia: repetir el mismo formulario (mismo `Idempotency-Key` generado al cargar) → misma respuesta sin duplicar
> 7. Tope mensual: registrar aportes que superen el tope → error 422 con mensaje legible en la UI
> 8. Consolidado: consultar por afiliado y rango → tabla con badge naranja en aportes marcados

Seguidamente se le indica a Claude que proceda con la realización del plan y que no finalice hasta que el proyecto quede completamente funcional (se le permite hacer pruebas en la terminal).

### Resumen de la implementación

Claude devuelve un resumen de lo ejecutado indicando:

> "Resumen de lo implementado:
>
> **Backend (22 archivos, 1007 líneas):**
> - `AporteRepositoryPort` / `SaldoRepositoryPort` — interfaces de dominio creadas
> - `RegistrarAporteUseCaseImpl` — idempotencia vía `Idempotency-Key` header, tope mensual configurable, marcación para revisión, `@Transactional` con optimistic lock
> - `ConsultarAportesUseCaseImpl` — query pura, `@Transactional(readOnly=true)`
> - `JpaAporteRepositoryAdapter` / `JpaSaldoRepositoryAdapter` — mapeo bidireccional Entity ↔ Domain
> - `GlobalExceptionHandler` — respuestas `{timestamp, codigo, mensaje}` para 400/409/422/500
> - `CorsConfig` — CORS habilitado para `localhost:5173`
> - `AporteController` actualizado: header `Idempotency-Key`, ruta `GET /{afiliadoId}/consolidado`, logging de auditoría
> - 6 tests unitarios (reglas de negocio con Mockito) + 5 tests de integración (MockMvc + H2)
> - `docker-compose.yml` con PostgreSQL 16
>
> **Frontend (5 archivos):**
> - `aportesApi.js` — `registrarAporte` + `consultarConsolidado` con fetch, manejo estructurado de errores del servidor
> - `index.css` — sistema de estilos con variables CSS, sin estilos inline
> - `RegistrarAporte.jsx` — validación cliente, campo fecha, idempotencia por envío, badge de revisión
> - `ConsolidadoAportes.jsx` — input `type=month`, badge visual ⚠ Revisión, `formatCOP` colombiano
> - Build Vite ✓ sin errores"

### Refinamiento visual del frontend

Se procede con la revisión de los archivos uno a uno, comenzando por el backend, seguido del frontend, al cual se le realizó un ajuste con ayuda de Claude indicándole que:

> "Mejora más el frontend haciendo uso de la skill ux-ui-pro-max para obtener un resultado visual más agradable y óptimo para el usuario"

El cuál respondió y cambió archivos:

> "Lo que cambió respecto al frontend anterior:
> - Navegación: Tabs de Radix UI en lugar del nav manual; transición suave entre pestañas con estado `data-[state=active]`
> - Formularios: Card + Label + Input + Select de shadcn; validación de campo con borde rojo en `border-destructive`
> - Alertas: componente Alert con ícono de Lucide, variantes `success` / `destructive` / `warning`
> - Tabla de consolidado: Table semántica con TableHeader/Body/Row/Cell, hover state en filas
> - Banner de total: diseño integrado en la misma Card con fondo `bg-primary`
> - Badges de estado: `Badge variant="warning"` con ícono TriangleAlert para revisión, `Badge variant="success"` para normal
> - Canal: ahora usa Select accesible de Radix en vez de `<select>` nativo
> - Header corporativo: banda azul de Protección S.A. con subtítulo en la parte superior"

### Cierre del Reto B

Finalmente verificamos que lo escrito por Claude esté correcto con lo sugerido en el plan. Una vez revisados, se procede con las pruebas manuales realizadas por el desarrollador y, al verificar que todo esté completo y de acuerdo al entregable, se da por finalizado el **Reto B**.
