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

### 2. ¿En qué momentos le llevaste la contraria al modelo, y por qué?

### 3. ¿Qué tradeoffs tomaste? ¿Qué dejaste por fuera a propósito?

### 4. ¿Qué falta para que esto sea apto para producción en un entorno SFC?
