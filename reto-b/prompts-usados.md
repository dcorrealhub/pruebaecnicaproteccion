# Notas de proceso — Prompts usados (Reto B)

## Prompt 1 — Generar el `AGENTS.md`

```
Vamos a trabajar el Reto B sobre el scaffold existente en reto-b/ (backend Spring Boot con
Clean Architecture + PostgreSQL/Flyway, y frontend React + Vite). Antes de escribir código,
quiero un AGENTS.md en la raíz del repo que fije el contexto y los estándares, para no repetirlos
en cada prompt.

Primero explorá el scaffold: paquetes del backend (domain/model, domain/port/in, domain/port/out,
application/usecase, infrastructure/persistence y web), la migración Flyway, el pom y la estructura
del frontend. Decime qué ya trae y qué está vacío.

Luego generá un AGENTS.md CONCISO y accionable (no un muro de texto) que cubra:

- Contexto: CIS Protección S.A., módulo de aportes voluntarios, afiliado sintético. Entorno
  regulado por la SFC: corrección, seguridad y trazabilidad no negociables. Datos SINTÉTICOS
  únicamente; nunca PII real.
- Stack y versiones reales del scaffold (Spring Boot, Java 21, PostgreSQL, Flyway, React 18, Vite,
  fetch nativo).
- Arquitectura: hexagonal / Clean Architecture, regla de dependencias (el dominio no conoce la
  infraestructura), puertos de entrada/salida, SOLID y separación comando/consulta (CQRS).
- Dinero: SIEMPRE BigDecimal, escala 2 y RoundingMode explícito; columnas NUMERIC(19,2); comparar
  con compareTo. Prohibido double/float para montos.
- Reglas de negocio en el dominio: monto > 0; tope mensual configurable por afiliado y mes; umbral
  de revisión que marca el aporte; mensajes de rechazo claros.
- Idempotencia: clave de idempotencia + restricción de unicidad en BD; ante reintento, devolver la
  respuesta original sin duplicar efectos.
- Validación: Bean Validation en el borde (@Valid, @NotBlank, @Positive, @Digits, enum para canal)
  MÁS revalidación de invariantes en el dominio.
- Persistencia: migraciones con Flyway (nada de ddl-auto=create-drop); constraints (NOT NULL,
  CHECK monto > 0, UNIQUE por afiliado+mes e idempotencia); @Transactional y control de
  concurrencia (bloqueo optimista/pesimista o UPDATE atómico condicionado al tope).
- Errores: @RestControllerAdvice, códigos HTTP correctos (201 + Location al crear; 400/404/409/422),
  contrato tipo ProblemDetail (RFC 7807), sin eco del input ni fuga de detalles internos.
- Observabilidad: logs sin datos sensibles (monto, afiliado); usar id de correlación.
- Tests: unitarios de dominio, de caso de uso e integración de persistencia; con casos de límite del
  tope, concurrencia e idempotencia.
- Anti-patrones a evitar (vienen de mi auditoría del Reto A, NO los repitas): double para dinero,
  comparar el tope con "==", SQL por concatenación, endpoints sin autenticación, exponer la entidad
  JPA en la API, registro no idempotente, saldo no segmentado por mes y falta de @Transactional.

Si el scaffold ya contradice alguna de estas reglas, marcámelo en una sección aparte en vez de
corregirlo callado. Entregame solo el AGENTS.md; todavía no implementes nada.

Dejá el AGENTS.md en la carpeta reto-b.
```

---

## Prompt 2 — Construir el Reto B

```
Leé primero AGENTS.md y respetalo como fuente de verdad; no repito el contexto acá.

Objetivo: implementar sobre el scaffold de reto-b/ la funcionalidad de registro y consulta de
aportes voluntarios, backend y frontend.

Alcance:
1. Registrar un aporte de un afiliado (id sintético): monto, fecha y canal. La operación debe ser
   IDEMPOTENTE.
2. Reglas: monto positivo; tope mensual por afiliado (parámetro configurable); un aporte que supere
   un umbral definido queda marcado para revisión; los que violen reglas se rechazan con mensaje
   claro.
3. Consultar el consolidado de aportes de un afiliado en un periodo: total y detalle.
4. Vista React: formulario para registrar un aporte y tabla con el consolidado. No tiene que ser
   bonita; tiene que ser correcta y razonable.

Antes de escribir código: revisá el scaffold, proponeme un plan corto (qué vas a tocar por capa),
listame los supuestos que vas a tomar y haceme las preguntas que necesites aclarar. Esperá mi OK
antes de implementar.

Cuando tengamos el plan, construí por rebanadas y mostrame cada una para revisarla: dominio y reglas
primero (con sus tests), después puertos y casos de uso, luego adaptadores de persistencia + migración
Flyway, después el controlador web con su manejo de errores, y al final el frontend. Incluí solo las
pruebas que consideres imprescindibles, pero que cubran el límite del tope, la idempotencia y, si es
viable, la concurrencia.

Dos cosas más:
- Si algo que te pido contradice AGENTS.md o una buena práctica, decime por qué antes de hacerlo; no
  lo ejecutes a ciegas.
- Dejá el diseño preparado para absorber un cambio de requisito a mitad (tope y umbral configurables,
  reglas de negocio aisladas en el dominio), porque es probable que te pida uno.
```

---

## Prompt 3 — Ajuste de requisito a mitad

```
Hay un ajuste que nos pidieron hacer:

"Negocio nos avisa que los aportes por sucursal tienen mayor riesgo. A partir de ahora, un aporte
por canal SUCURSAL se marca para revisión si supera 3.000.000. Los demás canales siguen con
5.000.000."

Realizalo guiandome en el proceso de qué debe irse haciendo y en el README incluí cómo tratamos ese ajuste.
```

**NOTA: ESTOS PROMPTS FUERON PASADOS/CORREGIDOS PRIMERO POR UNA IA PARA ASEGURAR UNA MAYOR EFECTIVIDAD**