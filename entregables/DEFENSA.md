# Defensa de Decisiones — Reto C
**Candidato:** Juan Esteban Valdes  
**Fecha:** 2026-06-26

---

## Reto A — Auditoría de código

### 1. ¿Cómo descompusiste el problema y cómo preparaste el contexto para la IA?

Le di a la IA un rol claro (Desarrollador Senior en una empresa financiera grande), el PDF de la prueba como contexto, y la instrucción explícita de ir paso a paso — separando el Reto A y el Reto B como tareas independientes. Le pedí que construyera un plan sólido antes de hacer cualquier cosa y que resolviera ambigüedades antes de ejecutar.

Para la auditoría, en lugar de pedirle que entregara todos los hallazgos de una, le pedí que los revisáramos archivo por archivo juntos. Esto me permitió profundizar en lo que no entendía, cuestionar lo que no me convencía, y aportar contexto de negocio que la IA no tenía (como la eliminación de los centavos por el Banco de la República). El resultado fue una auditoría construida en conjunto, no simplemente aceptada.

### 2. ¿En qué momentos le llevaste la contraria al modelo, y por qué?

El modelo quería entregar todos los hallazgos de una y avanzar rápido. Le puse freno: la auditoría no es un listado que se acepta a ciegas, es una exploración que requiere criterio propio. Le pedí que fuéramos archivo por archivo, que yo liderara la revisión y que él complementara — no al revés.

También discrepé en el tipo monetario: el modelo sugirió `BigDecimal` por defecto. Yo revisé el código, entendí que las operaciones eran solo sumas y comparaciones de pesos enteros, y apliqué el contexto del Banco de la República — la unidad mínima en Colombia es el peso, sin centavos. Con ese argumento de negocio, la decisión correcta era `long`, más simple y suficiente para este módulo, con una nota de evolución hacia `BigDecimal` si el sistema crece hacia cálculos con decimales.

### 3. ¿Qué tradeoffs tomaste? ¿Qué dejaste por fuera a propósito?

Decidí no proponer una migración a arquitectura hexagonal completa. La razón es falta de contexto suficiente: no sé si este sistema va a vivir muchos años, no conozco el roadmap del producto, y el nivel del autor del código sugiere que es alguien que está empezando. Imponer hexagonal sobre un junior sin la madurez técnica necesaria sería una curva de aprendizaje alta con beneficios inciertos en el corto plazo.

En cambio, propuse un equilibrio entre calidad y pragmatismo: señalar los problemas de acoplamiento y separación de capas como hallazgos claros, proponer la dirección correcta (separar entidades JPA del dominio, respetar responsabilidades por capa), pero sin sobrediseñar una solución que el equipo no está listo para mantener.

La arquitectura ideal sin el contexto del equipo y del negocio no es una decisión de ingeniería — es una imposición.

### 4. ¿Qué falta para que esto sea apto para producción en un entorno SFC?

Antes de hablar de código, faltan conversaciones:

1. **Reunión con negocio** — entender el alcance real del sistema: ¿cuántos afiliados?, ¿qué volumen de aportes?, ¿hay planes de expansión? Esto define si las decisiones de arquitectura actuales son suficientes o si hay que replantear. El desarrollador normalmente no entra a esta reunión, pero sus decisiones técnicas dependen de sus resultados.

2. **Revisión con el arquitecto** — con el contexto de negocio claro, definir cómo debe estar estructurado el sistema por dentro: qué tan lejos llevar la separación de capas, si la arquitectura hexagonal tiene sentido a futuro, qué componentes son candidatos a escalar de forma independiente.

Con eso resuelto, lo que falta a nivel técnico:

- **SonarQube** — análisis estático de calidad y seguridad integrado al pipeline. En un entorno SFC esto no es opcional.
- **Cobertura de pruebas** — definir y cumplir el umbral mínimo acordado por el equipo. Los hallazgos de la auditoría muestran que la cobertura actual es insuficiente.
- **IaC (Infrastructure as Code)** — sin esto no hay forma de trabajar en equipo de manera reproducible. Los entornos deben estar definidos como código para garantizar paridad entre dev, staging y producción.
- **Documentación** — al menos un contrato de API (OpenAPI/Swagger), decisiones de arquitectura registradas (ADRs), y guía de onboarding para nuevos desarrolladores.
- **Observabilidad** — logs estructurados, métricas y trazas distribuidas. En producción necesitas saber qué está pasando sin conectarte al servidor.
- **Gestión de secretos** — credenciales fuera del código, manejadas por un gestor de secretos (AWS Secrets Manager, Vault, etc.).

---

## Reto B — Construcción asistida

### 1. ¿Cómo descompusiste el problema y cómo preparaste el contexto para la IA?

Antes de empezar el Reto B, compacté la conversación del Reto A. Esto fue una decisión deliberada: la sesión anterior era larga y el modelo ya tenía demasiado contexto de la auditoría cargado. Compactar me permitió arrancar el Reto B con el contexto relevante preservado en memoria (decisiones técnicas, stack, criterios) pero sin el ruido de una sesión extensa. Es la misma lógica que hacer un commit antes de empezar una rama nueva — estado limpio, intención clara.

Usé dos modelos con roles distintos: **Opus para la planeación** — es el modelo más capaz, ideal para razonar sobre arquitectura, tradeoffs y decisiones técnicas complejas antes de escribir una línea de código — y **Sonnet para la implementación**, que es más rápido y económico para tareas de ejecución donde el plan ya está claro. Separar planeación de implementación no es solo una decisión de costo — es también una decisión de calidad: pensar antes de codificar produce mejores resultados que codificar mientras se piensa.

### 2. ¿En qué momentos le llevaste la contraria al modelo, y por qué?

Al momento de subir los cambios, el modelo quería hacer el merge directo a `master`. Le frené — la convención del repositorio es trabajar en ramas de candidato (`candidato/nombre-apellido`) y no tocar `master`. Subirlo directo habría mezclado mi entrega con el scaffold base, rompiendo la estructura que el evaluador espera. El modelo no tenía ese contexto de negocio; yo sí.

El modelo quería usar `BigDecimal` para el monto. En el Reto A había argumentado a favor de `long` — pero el Reto B es un módulo distinto, aislado, donde los valores son aportes voluntarios que sí pueden tener decimales en el futuro. Aquí le di la razón, pero por las razones correctas, no porque lo dijo la IA.

Cuestioné la semántica del tope mensual: el enunciado dice "supere", no "iguale". Le pedí al modelo que justificara su interpretación antes de codificar. Terminamos con `nuevoTotal > tope` (estrictamente mayor) porque "superar" en español no incluye el caso de igualdad exacta.

Para el `idempotenciaKey` el modelo lo iba a generar en el `onClick` del botón. Le pregunté qué pasaba si el usuario hacía doble click — el UUID cambiaría y se registraría el aporte dos veces. Lo corregimos: el UUID se genera en el `useState` inicial del componente (al montar), no al hacer click. Eso garantiza que reintentos del mismo formulario envíen el mismo key.

En el frontend propuso `step="0.01"` en el campo de monto. Los aportes son de millones, no centavos — un campo que sube de a un centavo en una app financiera colombiana no tiene sentido. Lo cambiamos a `step="100000"` con mínimo de $100.000.

### 3. ¿Qué tradeoffs tomaste? ¿Qué dejaste por fuera a propósito?

**TypeScript y Tailwind sobre el scaffold base:** El scaffold venía en JavaScript plano. Como la implementación partía desde cero — sin código previo que respetar, sin equipo que alinear — me di el lujo de migrar a TypeScript y agregar Tailwind. TypeScript aporta tipado estático que previene errores en tiempo de desarrollo y da más calidad al código. Tailwind permite construir UI consistente con clases inline sin mantener CSS separado. Si esto fuera un PR sobre código existente, ninguna de las dos cosas habría sido viable — el alcance habría sido implementar lo mínimo definido sin tocar el stack del equipo.

**`APORTE_REVERSADO` fuera de alcance:** El SQL del proyecto traía un tipo de evento `APORTE_REVERSADO`. Revisé el enunciado — no se pedía reversa. Implementarla sin requerimiento habría sido sobrediseño y habría introducido complejidad sin valor entregable. Quedó documentado en `NOTAS_PROCESO.md` como deuda técnica consciente.

**JWT en memoria del cliente, no en `localStorage`:** El token vive en una variable de módulo en el cliente. Al recargar la página se pierde y el usuario tiene que volver a hacer login. Es el tradeoff correcto para una app financiera: nada persistido que un atacante pueda robar con XSS. Para una sesión de jornada laboral (9 horas) el costo de re-autenticarse es bajo.

**Basic Auth → JWT:** Empezamos con Basic Auth por simplicidad, pero una credencial que viaja en cada request y no expira es un riesgo inaceptable en un sistema financiero. Migramos a JWT con expiración de 9 horas. El tradeoff es mayor complejidad de implementación a cambio de un vector de ataque cerrado. JWT fue el mínimo viable para este contexto: en Protección no se puede entregar un frontend y un backend sin ningún mecanismo de autenticación — exponer endpoints de aportes sin auth en una entidad financiera regulada no es una opción, así sea una prueba técnica.

**Credenciales obligatorias por variable de entorno:** Sin fallback en el código. Arrancar sin `API_USERNAME`, `API_PASSWORD` o `JWT_SECRET` configurados falla inmediatamente. Eso es intencional — un sistema financiero no debería poder arrancar sin autenticación configurada, ni siquiera en desarrollo.

**Canal hardcodeado como "WEB":** No se pedía selección de canal en el enunciado. Dejarlo como constante en el frontend es suficiente para el alcance y evita agregar campos sin requerimiento.

**Separación del paquete `auth`:** Los componentes de seguridad (`AuthController`, `JwtFilter`, `JwtUtil`, `SecurityConfig`, `UserDetailsConfig`) viven en `infrastructure.web.auth`, separados del resto de la capa web. La razón es cohesión: todo lo relacionado con autenticación y autorización cambia junto — si mañana se cambia el mecanismo de auth (OAuth2, API keys, otro proveedor), se toca un solo paquete sin afectar los controladores de negocio ni los DTOs. Mezclar clases de seguridad con controladores de dominio en el mismo paquete hace más difícil entender qué hace cada cosa y aumenta el riesgo de modificar lo que no corresponde.

### 4. ¿Qué falta para que esto sea apto para producción en un entorno SFC?

Lo mismo que en el Reto A aplica acá — las conversaciones con negocio y el arquitecto van primero. Adicionalmente, específico para este módulo:

- **Gestión de secretos real:** Las variables de entorno son el mínimo. En producción el `JWT_SECRET`, las credenciales de BD y las credenciales de API deberían vivir en un gestor de secretos (AWS Secrets Manager, HashiCorp Vault) con rotación automática. Un secreto que no rota es una vulnerabilidad latente.

- **HTTPS obligatorio:** JWT en texto plano sobre HTTP es equivalente a no tener seguridad. En producción el transporte debe ser TLS y el servidor debe rechazar conexiones HTTP.

- **Refresh tokens:** Un token de 9 horas que no se puede renovar obliga al usuario a re-autenticarse en mitad de la jornada si el sistema lo necesita más tiempo. Para producción se necesita un mecanismo de refresh con tokens de corta vida y refresh tokens de vida larga almacenados de forma segura.

- **Análisis de código estático:** Integrar SonarQube o similar al pipeline de CI. Detecta vulnerabilidades, code smells y cobertura de forma automática en cada PR. En un entorno SFC esto no es opcional.

- **Usuarios en base de datos:** El usuario actual está centralizado en memoria (InMemoryUserDetailsManager). En producción los usuarios deben estar en BD con contraseñas hasheadas, roles granulares y soporte para gestión del ciclo de vida (creación, bloqueo, expiración). Tener el usuario hardcodeado en el proceso es suficiente para una prueba técnica, pero no escala ni permite auditar accesos individuales.

- **Rate limiting en `/auth/login`:** Sin límite de intentos, el endpoint de login es vulnerable a fuerza bruta. Mínimo un límite por IP antes de exponer esto a internet.

- **Logs de auditoría:** Cada login exitoso, cada intento fallido, cada aporte registrado debería quedar en un log de auditoría inmutable. En un entorno SFC esto es un requerimiento regulatorio, no opcional.

- **Pruebas de integración y contrato:** Los tests actuales son unitarios. Falta cobertura del flujo completo: autenticación → registro de aporte → consulta de consolidado, incluyendo los casos de borde (tope excedido, periodo futuro, clave duplicada).

- **SonarQube y cobertura mínima definida:** Integrar SonarQube al pipeline de CI con un quality gate que bloquee merges si no se cumple el umbral de cobertura acordado por el equipo. El umbral mínimo no lo define el desarrollador — lo define el equipo junto con el área de calidad, con referencia estándar en 80% para módulos críticos. Sin esto, la cobertura es un número que nadie vigila.
