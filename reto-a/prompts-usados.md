# Prompts usados (Reto A)

## Prompt 1 — Apoyo en hallazgos + clasificación OWASP / CVSS

```
Contexto: estoy auditando como un Merge Request un módulo Spring Boot (Java 21, JPA, H2)
que registra aportes a un fondo voluntario. Corre en un entorno financiero regulado por la
SFC. Compila y los tests "felices" pasan. Te pego las clases (controller, service, entidades,
repos, DTO, application.properties, data.sql y el test).

Ya hice una primera lectura y tengo estas sospechas que quiero confirmar o descartar: SQL por
concatenación en el consolidado, el tope validado con "==" en vez de ">", dinero en double, el
no control de concurrencia, y que no hay idempotencia.

Quiero que me ayudes a:
1. Para CADA sospecha mía: dime si tengo razón, por qué, y señala la línea exacta. Si creo
   que algo es un bug y no lo es, corregime.
2. Dime qué se me pudo haber pasado (concurrencia, idempotencia, límites de Clean
   Architecture/CQRS, manejo de datos, OWASP), pero solo si lo puedes justificar con el código;
   no inventes hallazgos para rellenar.
3. Para los que confirmemos, propon una clasificación OWASP Top 10 2021 + CWE y un CVSS v3.1
   (vector y base score), y explicá el razonamiento del vector métrica por métrica. Voy a
   recalcular yo los vectores, así que quiero ver el porqué, no solo el número.

```

---

## Prompt 2 — Apoyo para construir el reporte

```
Tengo cerrados y verificados mis hallazgos de la auditoría del Reto A (te paso mi tabla: ID,
ubicación clase/línea, OWASP, CWE, CVSS vector y score, causa raíz y correctivas). Te adjunto
también un reporte de ejemplo de otra materia para que copies el nivel de formalidad y la
estructura de ficha, NO su contenido.

Ayudame a armar un reporte en Word (.docx) con: portada, tabla de contenido, resumen ejecutivo,
una matriz resumen de hallazgos, las fichas en detalle (una por hallazgo, mismo formato que el
ejemplo) y conclusiones con veredicto de MR y qué falta para producción en un entorno SFC.

Reglas:
- Usa SOLO mis hallazgos y mis textos; no agregues vulnerabilidades nuevas ni cambies mis
  severidades sin avisarme y explicar por qué.
- La severidad de la columna sigue mi CVSS; si notás una inconsistencia entre el puntaje y la
  criticidad de negocio, señalámela, no la "arregles" callado.
- Deja un espacio reservado (una caja) debajo de cada ficha para pegar yo las capturas.

Cuando termines, decime qué supuestos tomaste y qué dejaste por fuera a propósito.
```

**NOTA: ESTOS PROMPTS FUERON PASADOS/CORREGIDOS PRIMERO POR UNA IA PARA ASEGURAR UNA MAYOR EFECTIVIDAD**