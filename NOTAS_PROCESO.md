# Prompt Utilizados para el apoyo de la IA en la solución del reto-a
Después de haber identificado cuales eran los archivos en los que había mayor riesgo de errores como lo mencioné en HALLAZGOS.md me apoyé de la IA para identificar los problemas y revisar por mi misma si el resultado de la IA yo lo consideraba un problema en la revisión o no.

1. Analiza la siguiente clase Java como si estuvieras realizando una revisión de código para un Merge Request en un sistema financiero. Identifica problemas relacionados con buenas prácticas, SOLID, manejo de dinero, concurrencia, seguridad, validaciones y Clean Architecture. Para cada hallazgo explica el problema, el impacto y una posible recomendación. No inventes problemas que no estén presentes en el código. 

# Configuración para el apoyo de la IA en la solución del reto- b
Estoy usando Claude para el apoyo con esta prueba, en Claude cree un proyecto aislado para este segundo reto. Claude tiene un apartado de instrucciones para las conversaciones que estén asociadas con ese proyecto, así que ahí ingresé las tecnologias con las que estamos trabajando esta prueba, el contexto de lo que debía hacer en el reto.

## Prompts utilizados

1. " Estoy resolviendo una prueba técnica para un cargo Full Stack Senior. El proyecto ya incluye un scaffold con backend en Spring Boot 3.4, Java 21, PostgreSQL y frontend en React + Vite. La arquitectura esperada es Clean Architecture con principios SOLID y CQRS.

Quiero que actúes como un Tech Lead que revisa mi trabajo. No escribas la solución completa de inmediato. Primero ayúdame a analizar la estructura del proyecto, identificar la responsabilidad de cada capa y proponer un plan de implementación. Cuando implementemos código, explica el porqué de cada decisión y señala posibles mejoras o riesgos relacionados con concurrencia, validaciones, seguridad (OWASP), manejo de dinero con BigDecimal y buenas prácticas de Spring Boot."

2. " Antes de generar código, explícame qué clases deberían intervenir en esta funcionalidad siguiendo Clean Architecture. Describe la responsabilidad de cada una y justifica las dependencias entre dominio, aplicación, infraestructura y controlador. Implementemos una capa a la vez."

3. "Revisa esta implementación como si fueras un revisor técnico senior. Indica posibles problemas relacionados con SOLID, Clean Architecture, manejo de excepciones, concurrencia, OWASP Top 10 y mantenibilidad. No propongas cambios que no estén justificados."

4. Estoy configurando el entorno de desarrollo de un proyecto Spring Boot con PostgreSQL ejecutándose en Docker. El backend no logra conectarse a la base de datos. Ayúdame a diagnosticar el problema paso a paso. Indícame los comandos que debería ejecutar para verificar si el contenedor está en ejecución, qué puerto está exponiendo y cómo comprobar que la configuración del docker-compose.yml coincide con la configuración del backend. No asumas la causa del problema sin antes validar cada punto.

En este caso la IA propuso una secuencia de comandos para verificar el estado de los contenedores y los puertos configurados. Como yo ya sabía que tenia corriendo en los puertos de mi computador pude guiar a la guia para que me diera los comandos que necesitaba y así fue posible identificar que el problema estaba relacionado con la configuración del puerto de PostgreSQL y no con la implementación del backend.

Con **Perplexity** Verifiqué la paleta de Colores oficiales en las paginas de protección y contruí el prompt para Claude para que me ayudara a organizar el diseño de las vistas después de verificar que el backend estaba funcionando.

. Actúa como un diseñador senior UI/UX y front-end lead para una prueba técnica empresarial.
Ya tienes contexto del proyecto: estoy construyendo una aplicación con Java + Spring Boot en el backend y React en el frontend, y necesito que el diseño visual sea moderno, corporativo, limpio, confiable y muy profesional.

La identidad visual debe estar inspirada en la marca de Protección/PROTEX, usando esta paleta:

Primario: #94C11F

Primario oscuro: #6CB531

Gris corporativo: #B1B1B1

Negro: #000000

Blanco: #FFFFFF

Fondo claro: #F7F9F5

Texto secundario: #6B7280

Borde: #D1D5DB

Acentos opcionales: #FFB71B, #00A0DF, #E65014

Quiero que construyas una propuesta visual intachable, con estos criterios:

Debe verse como una solución de nivel corporativo/financiero, seria y moderna.

Debe ser visualmente limpia, con jerarquía clara y alto contraste.

Debe respetar la paleta, pero sin verse anticuada ni demasiado “verde”.

Debe incluir una guía para: layout general, navegación, cards, botones, inputs, tablas, alertas, badges, modales, estados hover/focus/active/disabled y empty states.

Debe recomendar tipografías, espaciados, radios, sombras, grid y sistema de componentes.

Debe proponer una experiencia visual consistente para desktop primero, pero adaptable a responsive.

Si algo no es ideal para una app moderna, corrígelo y propone una mejor alternativa.

Prioriza usabilidad, claridad y estética profesional por encima de adornos.

Además, quiero que me entregues la respuesta en este orden:

Dirección visual general.

Paleta aplicada a componentes.

Reglas de estilo para frontend.

Ejemplo de estructura para una pantalla principal.

Recomendación de librería o enfoque de implementación si aplica.

Checklist final para mantener consistencia visual.

No me des una explicación genérica. Quiero una propuesta accionable, concreta, lista para implementar en React. Si detectas contradicciones entre modernidad y marca, resuélvelas con criterio de diseño y explícame la decisión.

Cuando la IA me entregó su respuesta para generar el frontend me di cuenta de que la solución inicial no tenía en cuenta la normalización de los datos que ingresaba el usuario. Le expliqué a la IA que el ID del afiliado debía tratarse como un identificador único, sin importar si el usuario escribía AF-002, AF - 002 o AF- 002. Mi criterio fue que esos formatos representan el mismo afiliado y no tenía sentido que el sistema los interpretara como valores diferentes. Por eso decidí normalizar el dato desde el frontend utilizando .replace(/\s*-\s*/g, '-'), de manera que todas las consultas y registros se enviaran con el mismo formato y se evitaran errores por diferencias únicamente de escritura.

