# Prompts usados

Prompts con los que se trabajó la prueba con el asistente de IA (Claude Code), copiados tal como se escribieron.
Cuando el asistente hizo preguntas de aclaración antes de avanzar, se incluyen también las respuestas elegidas,
porque fueron decisiones de diseño. El razonamiento detrás de cada decisión está en
[`reto-b/NOTAS_PROCESO.md`](reto-b/NOTAS_PROCESO.md).

> Ningún prompt contiene datos reales: todos los afiliados y montos son sintéticos.

---

## Reto A · Auditoría de código

Se trabajó en una conversación aparte. Prompt inicial:

**Adjuntos:** `reto-a.zip`, `Prueba_Tecnica_Brief_Candidato 1.pdf`

> necesito que me ayudes a realizar esta prueba tecnica, te paso el codigo de la parte A, necesito que lo revises de
> acuerdo a las instrucciones del pdf y me digas que encontraste

**Resultado:** [`reto-a/HALLAZGOS.md`](reto-a/HALLAZGOS.md)

---

## Reto B · Construcción asistida

### 1. Implementación inicial

**1.1** · Adjuntos: `reto-b.zip`, `Prueba_Tecnica_Brief_Candidato 1.pdf`
> en el pdf que te pase esta una prueba tecnica que tengo que resolver, necesito que me hagas el reto b con las
> instrucciones del pdf y escribiendo codigo sobre el zip que te pase, si tienes dudas antes de empezar con el codigo
> me avisas

Respuestas a las preguntas de aclaración del asistente:

| Pregunta | Respuesta |
|---|---|
| ¿Dónde escribo el código de la solución? | En la carpeta `solucion reto b` |
| ¿Quién define la fecha del aporte? | El servidor usa la fecha actual |
| ¿Misma `idempotenciaKey` con datos distintos? | Rechazar con 409 Conflict |
| ¿Cómo interpreto los límites de tope y umbral? | Tope: `total + monto > tope` rechaza; umbral: `monto > umbral` marca |

### 2. Configuración local

**2.1**
> en la parte de application properties mencionas un docker compose pero no esta en el proyecxto, no voy a usar
> docker compose, usare una base de datos local de postgres, indicame como hago el arcxhivo .env para poner los datos
> de la base de datos

**2.2** · Se adjuntó el error de arranque `Port 8082 was already in use`
> pon el backend en el puerto 8080

### 3. Reglas de negocio y ajustes de requisito

**3.1**
> en el proyecto donde se encuentra esta parte? • Reglas de negocio: el monto debe ser positivo; existe un tope mensual
> por afiliado (parámetro configurable); un aporte que supere un umbral definido debe quedar marcado para revisión
> posterior. Aportes que violen las reglas se rechazan con un mensaje claro.

**3.2**
> pero en que parte de la base de datos esta el tope mensual?

**3.3** · Corrección de la interpretación del requisito
> lee bien el tope mensual es por afiliado

**3.4**
> no me aparece el tope mensual en la bd

**3.5**
> como lo corro sobre el contenedor de postgres

**3.6** · Se adjuntó una captura de la tabla `saldo_mensual`
> sigue sin aparecer el tope mensual

**3.7**
> el tope mensual lo crea por default?

**3.8**
> en que parte del rpyecto esta esto?  un aporte que supere un umbral definido debe quedar marcado para revisión
> posterior.

**3.9** · Ajuste de requisito
> haz que el umbral sea por usuario tambien

### 4. Frontend

**4.1**
> mejora el diseño del frontend para que se vea mas bonito

**4.2**
> pon los separadores de miles en aportes

**4.3**
> quita los comentarios de identificador sintetico y los puntos de miles

### 5. Despliegue en AWS

**5.1**
> necesito que subas eso a aws con el backend en ec2, el frontend en s3 y conectados a traves de cloudfront, ya tengo
> aws cli en la terminal, si tienes dudas antes de avanzar me avisas

Respuestas a las preguntas de aclaración del asistente:

| Pregunta | Respuesta |
|---|---|
| ¿En qué región de AWS despliego? | `us-east-2` |
| ¿Dónde va PostgreSQL? | En la misma EC2 (contenedor Docker) |
| ¿Qué tamaño de instancia EC2? | `t3.small`, 2 GB |

**5.2** · Ajuste de requisito de Negocio
> necesito que modifques esto en el codigo y en el deployment en aws Negocio nos avisa que los aportes por sucursal
> tienen mayor riesgo. A partir de ahora, un aporte por canal SUCURSAL se marca para revisión si supera 3.000.000. Los
> demás canales siguen con 5.000.000.

Respuesta a la pregunta de aclaración del asistente:

| Pregunta | Respuesta |
|---|---|
| Si un afiliado tiene su propio umbral y aporta por SUCURSAL, ¿qué umbral manda? | Manda el del canal |

**5.3**
> como me conecto a la db de ec2 desde datagrip?

**5.4**
> no puedes poner el puerto de la db publico y asignar la key acces.pem a la instancia de ec2?

Respuestas a las preguntas de aclaración del asistente (el asistente advirtió el riesgo de exponer los puertos):

| Pregunta | Respuesta |
|---|---|
| ¿Desde dónde se podrá entrar a los puertos 5432 y 22? | Todo internet (`0.0.0.0/0`) |
| ¿Qué habilito? | PostgreSQL y SSH con `access.pem` |

**5.5**
> dime la contraseña de la db

### 6. Documentación

**6.1**
> en el readme quita lo de scalfolds y pon la arquitecutra del aplicativo

**6.2**
> no quitaste defectos del scalfold en el readme

### 7. Revisión y comprensión del código

**7.1**
> aplicaste esto en el codigo?  criterio arquitectónico (Clean Architecture, SOLID, CQRS).

**7.2**
> explicame muy bien el codigo y los tests que hiciste

**7.3**
> para que son los models?

**7.4**
> donde se usan los models en que parte del codigo?

**7.5**
> para que sirven los ports y en que se diferencian de los dtos?

**7.6**
> que hay en la carpeta persistence? para que existe?

**7.7**
> explicame mejor los ports

**7.8**
> explicame los tests

### 8. Entrega en el repositorio

**8.1**
> copia y pega el contenido de reto b en la carpeta de retob qu viene del repo de github

**8.2**
> subelo a git en la ramaa juan-pablo-castano

**8.3**
> que mensaje pusiste en el commit?

**8.4**
> quita lo de coauthored by y pon el nombre de la rama como pide el repo y haz el push

**8.5** · Se adjuntaron capturas de los prompts iniciales de cada reto, el texto de los hallazgos del Reto A y el error de
render de Mermaid que mostraba GitHub en el README del Reto B
> necesito que en el root del proyecto me pongas prompts usados y pongas estos prompts y que me pongas la parte A del
> proyecto

**8.6**
> haz el commit y el push, antes de hacer el push dime el mensaje que vas a poner en el commit

**8.7**
> subelo asi

**8.8**
> en el markdwon de prompts usados pon todos los prompts que use en esta conversacion
