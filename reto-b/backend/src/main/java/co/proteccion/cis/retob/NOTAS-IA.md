# Notas de proceso — Uso de IA durante la prueba técnica

## Objetivo

Este documento registra de manera transparente cómo se utilizó la inteligencia artificial como herramienta de apoyo durante el desarrollo de la prueba técnica.

La IA fue utilizada como asistente técnico para análisis, revisión de arquitectura, generación de propuestas de implementación y validación de decisiones de ingeniería. Todas las decisiones finales, validaciones y modificaciones del código fueron revisadas antes de incorporarse al proyecto.

---

# Reto A — Auditoría de código

## Rol asignado a la IA

Actuar como un **Senior Software Engineer** realizando una revisión de Merge Request en un entorno financiero regulado, siguiendo principios de:

- Clean Architecture
- SOLID
- Clean Code
- OWASP Top 10
- Buenas prácticas de Spring Boot
- Contexto regulatorio de la Superintendencia Financiera de Colombia

---

## Prompt utilizado

```text
Actúa como un Senior Software Engineer encargado de revisar un Merge Request de una aplicación Spring Boot utilizada en un contexto financiero regulado.

El objetivo no es modificar el código, sino realizar una auditoría técnica identificando riesgos de arquitectura, seguridad, concurrencia, precisión numérica y cumplimiento de reglas de negocio.

Para cada hallazgo debes indicar:

- ubicación (archivo y línea cuando sea posible)
- severidad
- descripción
- impacto
- recomendación de corrección

Prioriza calidad sobre cantidad de hallazgos.

No implementes código.
```

---

## Resultado obtenido

La IA ayudó a identificar posibles riesgos relacionados con:

- precisión numérica en operaciones monetarias
- validación del tope mensual
- concurrencia
- idempotencia
- seguridad (inyección SQL)
- separación de responsabilidades
- configuración de desarrollo
- trazabilidad de auditoría

Los hallazgos fueron posteriormente revisados y documentados manualmente en:

```
reto-a/AUDITORIA.md
```

---

## Decisiones tomadas por el candidato

Durante la auditoría:

- Se validó cada hallazgo directamente sobre el código fuente.
- Se ajustó la severidad cuando fue necesario.
- No se realizaron modificaciones al código, ya que el reto solicitaba únicamente una auditoría.

---

# Reto B — Construcción asistida

## Rol asignado a la IA

Durante el desarrollo del Reto B la IA actúa como un **Pair Programming Assistant**, no como autor de la solución.

Su responsabilidad consiste en:

- ayudar a analizar el proyecto
- proponer alternativas de diseño
- revisar decisiones de arquitectura
- detectar posibles errores
- sugerir mejoras

La implementación final permanece bajo responsabilidad del candidato.

---

## Documentación de apoyo

La IA debe tomar como referencia obligatoria los siguientes documentos del proyecto:

```
reto-b/PROMPTS.md
```

Contiene:

- reglas de negocio
- arquitectura esperada
- restricciones
- forma de trabajo
- criterios técnicos

---

Durante toda la implementación deberá mantener actualizado:

```
reto-b/PLAN_IMPLEMENTACION.md
```

Este documento registra:

- análisis realizado
- decisiones tomadas
- archivos creados
- archivos modificados
- riesgos encontrados
- pendientes
- validaciones efectuadas

Cada fase implementada debe quedar documentada antes de continuar con la siguiente.

---

## Prompt utilizado

```text
Actúa como un Software Engineer Senior especializado en Java, Spring Boot, PostgreSQL, React, Clean Architecture y SOLID.

Trabaja únicamente sobre mi rama de GitHub.

Antes de modificar cualquier archivo:

- analiza completamente el proyecto
- revisa reto-b/PROMPTS.md
- documenta cada decisión en reto-b/PLAN_IMPLEMENTACION.md
- reutiliza el código existente siempre que sea posible
- evita duplicidad
- explica cada decisión técnica

Implementa la solución por fases.

En cada fase indica:

- archivos creados
- archivos modificados
- motivo
- decisiones tomadas
- cómo probar la funcionalidad

No implementes todo de una sola vez.

Mantén una arquitectura limpia y documenta todo el proceso.
```

---

## Metodología de trabajo

Para cada etapa del desarrollo se seguirá el siguiente flujo:

1. Analizar el código existente.
2. Identificar los componentes reutilizables.
3. Definir la estrategia de implementación.
4. Documentar el análisis.
5. Implementar únicamente la fase correspondiente.
6. Ejecutar pruebas.
7. Actualizar la documentación.
8. Continuar con la siguiente fase.

---

## Registro de cambios de requisito

En caso de que durante la prueba se introduzca un nuevo requerimiento funcional, se documentará:

- descripción del cambio
- impacto sobre la arquitectura
- componentes afectados
- estrategia adoptada
- motivo de la decisión

---

## Observaciones

La inteligencia artificial fue utilizada exclusivamente como herramienta de apoyo para análisis, documentación y acompañamiento técnico.

Todas las decisiones finales sobre diseño, implementación y validación fueron revisadas por el candidato antes de incorporarse al proyecto.