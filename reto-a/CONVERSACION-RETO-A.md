# Conversación — Reto A (auditoría del módulo de aportes)

Registro de la conversación con el asistente de IA (Claude Code) durante la auditoría de `reto-a`. Contiene los mensajes del usuario y del asistente en orden. Las acciones que hizo el asistente con herramientas (leer archivos, ejecutar tests, editar) aparecen resumidas en bloques *«Acciones»*.

**Archivos producidos:**
- [HALLAZGOS.md](HALLAZGOS.md): informe técnico completo.
- [HALLAZGOS-RESUMEN.md](HALLAZGOS-RESUMEN.md): tabla en lenguaje sencillo.
- Comentarios `// H-0xx` en el código fuente.

---

## 1. Usuario — Prompt inicial de revisión

> Actúa como un arquitecto de software senior y revisor de código experto en Java, Spring Boot, JPA, concurrencia, Clean Architecture, SOLID y testing.
>
> ## Contexto
> El proyecto es un módulo pequeño de Spring Boot cuyo servicio es el registro de aportes a un fondo voluntario. En teoría todo funciona y tiene tests de los caminos felices. Se va a agregar el siguiente código al proyecto: <PEGAR CÓDIGO / DIFF / RUTA DEL REPOSITORIO>.
>
> Parte de esta premisa: que "funcione" y que pasen los tests de camino feliz no garantiza que sea correcto. Se manejan dineros de afiliados, así que cualquier error de cálculo, duplicidad o inconsistencia tiene impacto real.
>
> ## Cómo debes trabajar
> 1. **Comprensión primero (no reportes nada todavía).** Recorre todo el código: build, configuración, paquetes, entidades, migraciones y tests. Traza el flujo completo de registrar un aporte (request → controller → validación → servicio → dominio → repositorio → BD → respuesta). Identifica la arquitectura real y las invariantes de negocio que el código asume (monto válido, afiliado activo, no duplicar aportes, saldo consistente, etc.) y dónde se garantizan o no. Escribe ese entendimiento como primera sección del informe.
> 2. **Revisión carpeta por carpeta y archivo por archivo**, aplicando todas las dimensiones de abajo.
> 3. **Revisión transversal**: problemas que cruzan archivos (transacciones, condiciones de carrera, violaciones de la regla de dependencia, inconsistencias entre entidad, migración y DTO, invariantes sin test).
> 4. **Consolidación**: elimina duplicados, re-valida cada hallazgo y ordénalos por severidad.
>
> ## Dimensiones a revisar (no te limites a errores de código)
> - **Lógica de negocio**: uso de BigDecimal (escala, redondeo, compareTo), validación de montos (negativos, cero, nulos, máximos), idempotencia ante reintentos o doble envío, manejo de fechas y zona horaria, estados y transiciones, reglas de negocio ausentes.
> - **Concurrencia y transacciones**: actualizaciones perdidas de saldo (sin @Version o bloqueo), check-then-act sin restricción única, @Transactional mal ubicado (métodos privados, auto-invocación, ausente), rollback con excepciones checked, llamadas externas o eventos dentro de la transacción, estado mutable en beans singleton.
> - **Arquitectura / Clean Architecture**: dependencias del dominio hacia framework o infraestructura, entidades JPA expuestas en la API, lógica en controllers o repositorios, servicios con demasiadas responsabilidades, contratos HTTP (códigos de estado, versionado).
> - **SOLID y patrones de diseño**: violaciones concretas de cada principio, inyección por campo, new de dependencias, condicionales por tipo que deberían ser Strategy, ausencia de Value Objects (Monto, Documento) cuando resuelvan un problema real.
> - **Clean code**: nombres, métodos largos, números mágicos, código muerto, Optional mal usado, excepciones genéricas, Lombok peligroso en entidades JPA (@Data, @EqualsAndHashCode, @ToString).
> - **Persistencia**: N+1, EAGER, open-in-view, índices, restricciones que existen en Java pero no en la BD, ddl-auto vs migraciones, paginación.
> - **Manejo de errores y validación**: @RestControllerAdvice, respuestas consistentes, detalles internos expuestos, Bean Validation.
> - **Seguridad**: autenticación/autorización, IDOR, datos personales en logs, secretos en configuración, inyección.
> - **Observabilidad y auditoría**: trazabilidad de quién registró qué aporte, logs con contexto, métricas, timeouts.
> - **Tests**: casos de error y límite ausentes, tests sin asserts relevantes o que solo prueban mocks, falta de tests de integración (@WebMvcTest, @DataJpaTest), H2 vs motor real, ausencia de tests de concurrencia, tests frágiles, invariantes sin cubrir.
>
> ## Criterio para que algo sea un hallazgo
> Solo es hallazgo si puedes explicar **qué falla o se degrada, en qué escenario concreto y qué le cuesta a la organización**. Si es solo otra forma válida de hacerlo, es una decisión de diseño: no lo reportes como hallazgo, ponlo en "Observaciones descartadas" con una línea de justificación. No inventes reglas de negocio ni normativa: si no puedes verificarlas en el código, repórtalas como "Preguntas abiertas". Si falta una clase referenciada, dilo en vez de suponer su contenido.
>
> ## Severidad (impacto en la organización)
> - **Crítica**: pérdida o descuadre de dinero, corrupción de datos, brecha de seguridad o incumplimiento normativo. Bloquea el merge.
> - **Alta**: fallo probable en producción en condiciones normales (concurrencia, reintentos, datos límite) o deuda que impide evolucionar el módulo.
> - **Media**: problema real pero acotado (mantenibilidad, rendimiento futuro, tests insuficientes).
> - **Baja**: legibilidad y consistencia.
> - **Informativa**: mejora opcional sin riesgo.
> La severidad se asigna por impacto × probabilidad, no por categoría.
>
> ## Entregable
> Un único archivo Markdown con esta estructura:
> 1. **Contexto entendido** (qué hace el código, flujo, arquitectura, invariantes).
> 2. **Resumen ejecutivo**: conteo por severidad y veredicto (Apto / Apto con cambios / No apto para merge).
> 3. **Tabla de hallazgos**: ID | Severidad | Categoría | Ubicación | Título.
> 4. **Detalle de cada hallazgo** (H-001, H-002, ...):
>    - **Ubicación**: ruta completa de carpeta y archivo, método y líneas.
>    - **Severidad**.
>    - **Categoría**.
>    - **Evidencia**: fragmento mínimo de código.
>    - **Motivo**: por qué es un problema y no una decisión de diseño, lógica o arquitectura; escenario concreto e impacto.
>    - **Solución**: cómo corregirlo, con código de ejemplo y alternativas si las hay.
>    - **Test que lo demostraría**: el caso de prueba que hoy falta o fallaría.
> 5. **Preguntas abiertas**.
> 6. **Observaciones descartadas**.
> 7. **Plan de acción sugerido** (orden de corrección).
>
> Un problema por hallazgo, sin duplicados (si se repite en varios archivos, un solo hallazgo con varias ubicaciones). Sé exhaustivo: prefiero un informe largo y preciso a uno corto y genérico.
>
> ---
>
> Termina de realizar la tarea, pero ten en cuenta el priorizar lo que un revisor bloquearía antes de aprobar el MR. No buscamos cantidad de hallazgos, sino criterio. Por ejemplo, si yo revisara el controller de primeras, solo por eso lo descartaría porque esta mapeando directamente ahí, y además esta ejecutando un SQL directamente desde el controller.

### Acciones del asistente
1. Cargó la skill `revision-codigo-spring`, que define el flujo: comprensión → revisión por archivo → revisión transversal → consolidación.
2. Leyó el `README.md`, `docker-compose.yml` y los 13 archivos de `reto-a`: `pom.xml`, el controller, el servicio, las entidades, el DTO, los repositorios, `application.properties`, `data.sql` y el test.
3. Copió el módulo a un directorio temporal, **sin modificar el original**, y escribió un test de verificación (`VerificacionHallazgosTest`) para reproducir los problemas. Resultados:
   - Tope: AF-003 (4,5 M) + 6 M = 10,5 M → **aceptado**. Si el total da exactamente 10 M → **rechazado**.
   - Inyección: `afiliadoId = x' OR '1'='1' --` → devuelve aportes de AF-001 **y** AF-002.
   - Concurrencia: 50 aportes simultáneos de $1.000 → suma de aportes $50.000, **saldo $9.000**.
   - `double`: 10 × 0,1 = `0.9999999999999999`. `monto: 1e400` → HTTP 200 y saldo `Infinity`.
   - Dos filas de saldo del mismo afiliado → `IncorrectResultSizeDataAccessException` (500).
   - Monto negativo y afiliado nulo → excepción no manejada (500).
   - La suite original `AporteServiceTest` sigue pasando.
4. Redactó `reto-a/HALLAZGOS.md`, la ruta que indica el README.

### Asistente

El informe está en [reto-a/HALLAZGOS.md](HALLAZGOS.md), que es la ruta que pide el README. El veredicto es **No apto para merge**. Reproduje los problemas más graves en una copia aparte del módulo, sin tocar el código; la suite original sigue pasando con todos ellos presentes.

**Lo que bloquearía el MR en minutos:**
- **El controller (tu punto, que separé en dos hallazgos):**
  - **H-001 · Inyección SQL (crítica):** pasando `afiliadoId = x' OR '1'='1' --` la consulta devolvió los aportes de todos los afiliados.
  - **H-007 · Capas rotas (alta):** el controller usa `JdbcTemplate` y su propio mapeo de filas, saltándose el servicio. Además duplica `findByAfiliadoIdAndPeriodo`, un método parametrizado que ya existe y nadie usa.
- **H-002 · El tope mensual no se aplica:** se compara con `==`. Con un saldo de 4,5 M, un aporte de 6 M (total 10,5 M) se acepta, pero uno que deja el total en exactamente 10 M se rechaza.
- **H-003 · Saldo que se pisa con aportes simultáneos:** con 50 aportes concurrentes de $1.000, el saldo quedó en $9.000 en vez de $50.000.
- **H-004 · Registro sin transacción:** el saldo, el evento y el aporte se guardan por separado. Si falla el último paso, el saldo queda inflado sin aporte detrás.
- **H-005 · Dinero en `double`:** diez aportes de 0,10 dan un saldo de `0.9999999999999999`, y un `monto: 1e400` responde 200 y deja el saldo en `Infinity` para siempre.
- **H-006 · El saldo "del mes" ignora el mes:** los aportes de 2026-10 se suman a la fila de 2025-06. Si existe una fila de otro mes para el mismo afiliado, todos sus aportes fallan con 500.

Después vienen 5 hallazgos altos: falta de idempotencia, ausencia de autenticación (cualquiera consulta a cualquier afiliado cambiando el id), fecha y periodo que dependen de la zona horaria del servidor, tests que no cubren ninguna regla de dinero y el controller ya citado. Para mantener el criterio sobre la cantidad hay solo 6 medios y 1 bajo. Puse en "Observaciones descartadas" lo que es una decisión de diseño válida y en "Preguntas abiertas" las reglas que no puedo confirmar desde el código, como el umbral de revisión por aporte o por acumulado y si se debe mostrar al afiliado que su aporte quedó marcado para revisión.

No hice commit ni creé la rama `candidato/...` que pide el README.

---

## 2. Usuario — Comentarios en el código

> okay agrega comentarios específicos en los lugares de los hallazgos con el siguiente patrón: "H-01" y un pequeño texto que lo describa

### Acciones del asistente
1. Agregó comentarios de una línea en el lugar exacto de cada hallazgo, en 11 archivos, con la sintaxis de comentario de cada tipo de archivo: `//` en Java, `#` en `.properties`, `--` en SQL y `<!-- -->` en el `pom.xml`.
2. Comprobó con `git diff` que **no se borró ni modificó ninguna línea existente** (solo se agregaron comentarios), que aparecen los 18 IDs y que `mvn test` sigue pasando.

### Asistente

Agregué comentarios en 11 archivos, en las líneas exactas de cada hallazgo, por ejemplo:

```java
// H-002: compara con == ; cualquier acumulado mayor al tope se acepta y el exacto se rechaza.
if (nuevo == topeMensual) {
```

**Formato:** usé `H-001` (tres dígitos) en lugar de `H-01` para que coincida con los IDs de `HALLAZGOS.md`. Si prefieres dos dígitos, cambio los comentarios y el informe juntos para que sigan cuadrando.

**Dónde están:**
- **Código Java:** `AporteController.java`, `AporteService.java`, las tres entidades, `AporteRequest.java` y `SaldoJpaRepository.java`.
- **Configuración y build:** `application.properties` con `#`, `data.sql` con `--` y `pom.xml` con `<!-- -->`, para H-007 y H-009.
- **Tests:** `AporteServiceTest.java`, con la cabecera de H-011.

Aparecen los 18 IDs al menos una vez. Cuando un hallazgo afecta varios archivos (H-005 por el `double`, H-018 por el `@Data`), lo marqué en cada lugar.

El diff no borra ni modifica ninguna línea existente: solo agrega comentarios, y `mvn test` sigue pasando.

Ten en cuenta que el README pide no modificar el código del módulo auditado. Estos comentarios no cambian el comportamiento, pero si el evaluador lo interpreta de forma estricta, conviene dejarlos en un commit separado de `HALLAZGOS.md`. Todavía no hice commit ni creé la rama.

---

## 3. Usuario — Explicación de H-003, H-004 y H-006

> Okay por aparte, necesito que me expliques un poco mejor el 3, 4, 6. No cambies nada del MD, solo explicamelo

### Asistente

Los tres afectan al saldo, pero de forma distinta. **H-003** trata de dos peticiones que llegan a la vez, **H-004** de una petición que falla a mitad de camino y **H-006** de usar la fila de saldo equivocada.

#### H-003 — Actualización perdida del saldo (concurrencia)

El servicio actualiza el saldo en tres pasos separados: **lee** el valor, **calcula** el nuevo en memoria de Java y **escribe** el resultado.

```java
Saldo s = saldoRepo.findByAfiliadoId(...);   // 1. leer
double nuevo = s.getTotalMes() + monto;      // 2. calcular en memoria
s.setTotalMes(nuevo); saldoRepo.save(s);     // 3. escribir
```

Nada impide que otra petición del mismo afiliado lea el saldo entre el paso 1 y el 3. Por ejemplo, con AF-001 en saldo 0 y dos aportes de $1.000 al mismo tiempo (uno desde la app y otro desde la web):

| Momento | Petición A | Petición B | Saldo en BD |
|---|---|---|---|
| t1 | lee saldo = 0 | | 0 |
| t2 | | lee saldo = 0 | 0 |
| t3 | calcula 0 + 1.000 | calcula 0 + 1.000 | 0 |
| t4 | escribe 1.000 | | 1.000 |
| t5 | | escribe 1.000 | **1.000** |

Se guardan dos aportes de $1.000 en la tabla `aporte`, pero el saldo queda en $1.000 y no en $2.000. La escritura de B pisó la de A porque B calculó su valor con un dato viejo. A esto se le llama *lost update*. En la prueba que corrí, 50 aportes simultáneos dejaron el saldo en $9.000 en vez de $50.000.

**El tope también se puede saltar por esta vía.** Si AF-003 tiene 4,5 M y llegan a la vez dos aportes de 5 M, cada uno ve 4,5 M y calcula 9,5 M, que está por debajo del tope. Los dos se aceptan, aunque en realidad suman 14,5 M. Esto pasaría incluso con el `==` ya corregido.

**Por qué `@Transactional` no basta:** con el aislamiento por defecto (READ COMMITTED), las dos transacciones siguen leyendo el mismo 0, porque ninguna ha escrito todavía. La transacción agrupa las escrituras, pero no impide que otra transacción lea el mismo dato al mismo tiempo. Hay tres formas de impedirlo:
- **`@Version` (bloqueo optimista):** B intenta guardar, Hibernate detecta que la versión cambió desde que leyó y lanza una excepción. B reintenta con el dato fresco.
- **`SELECT ... FOR UPDATE` (bloqueo pesimista):** B se queda esperando hasta que A termine, y entonces lee 1.000.
- **Update atómico en SQL:** `UPDATE saldo SET total_mes = total_mes + :monto WHERE ... AND total_mes + :monto <= :tope`. La BD suma sobre el valor real de la fila, y la misma sentencia valida el tope.

#### H-004 — Sin `@Transactional`: tres commits independientes

H-003 trata de dos peticiones que chocan. H-004 trata de **una sola petición que falla a la mitad**.

Cada `save()` de Spring Data abre y confirma su propia transacción mientras no haya una transacción exterior. Como `registrar()` no tiene `@Transactional`, el método hace tres commits separados:

```
saldoRepo.save(s)        → COMMIT ✔  (el saldo ya subió)
eventoRepo.save(evento)  → COMMIT ✔  (el evento ya dice "APORTE_REGISTRADO")
aporteRepo.save(aporte)  → 💥 falla (BD caída, timeout, pool agotado, restricción...)
```

Lo que ya se confirmó no se deshace, así que queda:
- un saldo incrementado en el monto,
- un evento de auditoría que afirma que el aporte se registró,
- **ningún aporte** en la tabla `aporte`,
- y el cliente recibiendo un error.

El cliente reintenta, como es normal ante un error, y esta vez todo sale bien. Ahora el saldo tiene el monto **dos veces** pero solo existe un aporte, y la suma de los aportes ya no coincide con el saldo. Nadie lo detecta, porque no hay ningún proceso de conciliación.

Con `@Transactional` en el método público, los tres `save` se unen a una sola transacción. Si el último falla, la BD hace rollback de todo y queda como si la petición nunca hubiera llegado. O se registra todo o no se registra nada.

**Resumen de la diferencia:** H-004 se arregla con `@Transactional` y H-003 no; necesita bloqueo. Por eso hay que corregir los dos.

#### H-006 — El saldo «del mes» se busca ignorando el mes

Según su modelo, la tabla `saldo` guarda **un acumulado por afiliado y por mes**:

```java
private double totalMes;   // "Acumulado del mes"
private String mes;        // "YYYY-MM"
```

La búsqueda, en cambio, no usa el mes:

```java
Optional<Saldo> findByAfiliadoId(String afiliadoId);   // trae "la" fila del afiliado, sea del mes que sea
```

Además, nada en el código crea la fila del mes nuevo. Eso produce tres efectos.

**1. Los aportes caen en el mes equivocado.** `data.sql` solo crea filas de `2025-06`. Si hoy (octubre de 2026) AF-001 aporta, ocurre esto:
- El aporte se guarda con `periodo = 2026-10`, porque lo calcula con `LocalDate.now()`.
- El saldo que se incrementa es la fila de `2025-06`, porque es la única que existe.

El reporte de junio de 2025 cambia con dinero de octubre de 2026, así que el saldo de un mes cerrado queda alterado.

**2. El tope «mensual» se vuelve un tope de por vida.** Como la fila nunca se reinicia ni se crea una nueva por mes, `totalMes` acumula todo lo que el afiliado ha aportado en su historia. Supongamos que un afiliado aportó 3 M en cada uno de tres meses. Lleva 9 M en la misma fila, y al cuarto mes un aporte de 2 M lo pone en 11 M. Con el tope corregido (`>`), ese aporte **se rechazaría**, aunque en ese mes solo ha aportado 2 M. Es decir, arreglar H-002 sin arreglar H-006 empieza a rechazar aportes legítimos.

**3. Si alguien crea la fila del mes nuevo, el afiliado queda bloqueado.** Lo natural en un modelo mensual es tener una fila por mes. En cuanto AF-001 tiene dos filas (`2025-06` y `2025-07`), `findByAfiliadoId` espera un único resultado y encuentra dos. Lanza `IncorrectResultSizeDataAccessException` y **todos los aportes de ese afiliado fallan con 500** desde ese momento. Lo verifiqué en la prueba.

**Corrección:** buscar por `(afiliadoId, mes)`, con el mes calculado una sola vez a partir de la fecha de negocio (lo que conecta con H-010). Si la fila del mes no existe, se crea, y una restricción `UNIQUE(afiliado_id, mes)` en BD evita que dos peticiones concurrentes creen dos filas del mismo mes.

**Cómo encajan los tres:** para que el saldo sea confiable hay que asegurar que se use la fila correcta (H-006), que se escriba completo o no se escriba nada (H-004) y que una escritura no pise a otra (H-003). Con uno solo que falle, el saldo deja de cuadrar con la suma de los aportes.

---

## 4. Usuario — Columna «Cómo lo corregirías» y versión entendible

> Okey modifica ese MD, pero hagamos otro que solo tenga la tabla, porque omitiste una columna que seria "como lo corregirías", y pues en general trata de hacerlo mas entendible para cualquier persona.

### Acciones del asistente
1. En `HALLAZGOS.md` agregó la columna **«Cómo lo corregiría»** a la tabla de la sección 3, sin cambiar el resto del informe.
2. Creó `HALLAZGOS-RESUMEN.md`, que solo contiene la tabla, en lenguaje sencillo y con ejemplos concretos.

### Asistente

Hice dos cambios: agregué la columna que faltaba al informe y creé un documento aparte que solo tiene la tabla, escrito para que lo entienda cualquier persona.

1. **[HALLAZGOS.md](HALLAZGOS.md):** la tabla de la sección 3 ahora tiene la columna **«Cómo lo corregiría»**, con la solución técnica resumida de cada hallazgo. No toqué nada más del informe.

2. **[HALLAZGOS-RESUMEN.md](HALLAZGOS-RESUMEN.md):** documento nuevo con una sola tabla de 6 columnas:
   - **ID:** el mismo del informe, para poder cruzarlos.
   - **Gravedad:** con una leyenda en una línea al inicio.
   - **Dónde está:** la parte del sistema, además de la clase.
   - **Qué pasa:** el problema sin jerga técnica.
   - **Ejemplo de lo que puede salir mal:** un caso concreto con cifras. Los que reproduje en las pruebas dicen *Comprobado*.
   - **Cómo lo corregiría:** la solución contada de forma sencilla. El nombre técnico aparece solo cuando ayuda, como `@Transactional` o `BigDecimal`.

Interpreté tu mensaje como hacer las dos cosas. Si solo querías el resumen y prefieres que `HALLAZGOS.md` quede como estaba, quito la columna. No hice commit.

---

## 5. Usuario — Registro de la conversación

> Generame otro markdown donde muestres toda nuestra conversacion, del reto a ahi

### Asistente

Se generó este archivo, `CONVERSACION-RETO-A.md`.
