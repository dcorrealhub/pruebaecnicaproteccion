# Hallazgos de Auditoría – Reto A

## Metodología utilizada

Se realizó una revisión manual del código fuente siguiendo los siguientes criterios:

* Seguridad de la aplicación (OWASP Top 10).
* Manejo de información financiera.
* Validaciones de negocio.
* Consistencia de datos.
* Buenas prácticas en Spring Boot y JPA.

Adicionalmente, se utilizó asistencia de IA como apoyo para complementar la revisión y contrastar posibles riesgos. Todos los hallazgos documentados fueron posteriormente verificados directamente en el código fuente.

---

## Prompts utilizados durante la auditoría

### Revisión general del proyecto

> Analiza un proyecto Spring Boot relacionado con aportes financieros e identifica posibles problemas de seguridad, manejo de dinero, validaciones de negocio, integridad de datos, concurrencia y consistencia transaccional. Explica el riesgo y la recomendación.

**Objetivo:** obtener una visión general de riesgos potenciales antes de revisar el código manualmente.

---

### Auditoría orientada a entorno financiero

> Analiza el código como si estuvieras revisando un Merge Request para una entidad financiera. Prioriza problemas relacionados con precisión numérica, manejo de dinero, consistencia transaccional, concurrencia, idempotencia y seguridad. Clasifica los hallazgos por severidad.

**Objetivo:** enfocar la revisión en riesgos relevantes para un contexto regulado.

---

### Búsqueda de vulnerabilidades de seguridad

> Revisa el código buscando posibles vulnerabilidades OWASP Top 10. Identifica riesgos de SQL Injection, validación insuficiente de entradas, exposición de información sensible o cualquier patrón inseguro. Explica el impacto y la mitigación recomendada.

**Objetivo:** identificar problemas de seguridad que podrían bloquear la aprobación de un Merge Request.

---

### Revisión de manejo de dinero

> Analiza el uso de tipos numéricos dentro del proyecto. Identifica posibles riesgos asociados al uso de double, float o comparaciones numéricas incorrectas en procesos financieros. Explica las mejores prácticas recomendadas.

**Objetivo:** validar la corrección de cálculos monetarios.

---

### Revisión de concurrencia y consistencia

> Analiza los procesos de actualización de saldo y persistencia de aportes. Identifica posibles condiciones de carrera, pérdida de actualizaciones o inconsistencias provocadas por accesos concurrentes. Explica alternativas de mitigación utilizando capacidades de JPA y Spring.

**Objetivo:** detectar riesgos de concurrencia y pérdida de información.

---

### Revisión de transacciones

> Identifica operaciones que deberían ejecutarse dentro de una misma transacción. Explica qué inconsistencias podrían ocurrir si una operación falla después de haberse persistido parcialmente el estado.

**Objetivo:** validar atomicidad y consistencia de los procesos críticos.

---

### Estrategia utilizada durante la auditoría

Durante la revisión se utilizó el siguiente enfoque:

**Analizar → Contrastar → Verificar → Documentar**

La IA fue utilizada para sugerir posibles riesgos y líneas de análisis. Ningún hallazgo fue documentado sin ser verificado posteriormente en el código fuente. La clasificación de severidad, evidencia y recomendaciones fueron definidas después de revisar manualmente cada caso identificado.
