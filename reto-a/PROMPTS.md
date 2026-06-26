# PROMPTS.md — Bitácora de Ingeniería de Prompts

**Módulo:** Reto A — Registro de Aportes Voluntarios
**Proyecto:** Prueba Técnica AI-First — CIS Protección S.A.
**Rama:** `candidato/tomas-rios`
**Fecha:** 2026-06-26

---

## Introducción

Este archivo forma parte de la directriz **"Conserva tus prompts, tus notas y tu razonamiento"** del brief de la prueba técnica del Centro de Ingeniería de Software (CIS) de Protección S.A.

En el marco de la cultura **AI-First** del CIS, cada interacción con el agente de IA es tratada como un activo de ingeniería: los prompts no son instrucciones informales, sino artefactos diseñados con precisión para producir salidas reproducibles, auditables y alineadas con los marcos técnicos y regulatorios del equipo. Este documento registra los prompts maestros utilizados en las Fases 0, 1 y 2 del proceso de refactorización del módulo `reto-a`, junto con las decisiones de diseño del prompt que justifican su estructura.

---

## Prompt N° 1 — Auditoría Avanzada de Código (Fase 0)

### Propósito
Obtener un análisis crítico exhaustivo del código fuente del módulo, produciendo hallazgos estructurados que un revisor de MR senior bloquearía antes de paso a producción.

### Prompt completo utilizado

```
Actúa como un Principal Software Engineer y Auditor de Código Senior para el Centro de
Ingeniería de Software (CIS) de Protección S.A. Estamos bajo un marco de cultura AI-first,
Clean Architecture, SOLID, prácticas DevSecOps y separación estricta de Comando/Consulta
(CQRS). Nuestro sistema opera en un entorno financiero altamente regulado por la
Superintendencia Financiera de Colombia (SFC), por lo que la precisión numérica, la seguridad,
la inmutabilidad, la trazabilidad y la idempotencia no son negociables.

El código fuente de un módulo de Spring Boot funcional que actúa como un servicio de registro
de aportes a un fondo voluntario se encuentra en la carpeta "reto-a". El código compila y los
caminos felices ("happy paths") de los tests pasan, pero contiene fallas críticas de diseño,
vulnerabilidades y malas prácticas ocultas que un revisor de Merge Request (MR) senior
bloquearía inmediatamente antes de pasar a producción.

Analiza críticamente el código que te suministraré a continuación y genera una lista de
hallazgos detallada. Para cada hallazgo detectado, debes seguir estrictamente la siguiente
estructura de salida en Markdown:

### Hallazgo N° [Número]: [Título corto y descriptivo del problema]
*   **Ubicación:** [Clase, Método y líneas de código afectadas]
*   **Severidad:** [Crítica / Alta / Media / Baja]
*   **Por qué es un problema:** [Explicación técnica detallada]
*   **Cómo lo corregirías:** [Estrategia de mitigación y refactorización]

Especialmente, busca y prioriza fallas en los siguientes vectores:
1. Corrección Numérica: Uso incorrecto de tipos de datos flotantes (double/float) para dinero
   en lugar de BigDecimal con escala adecuada.
2. Idempotencia y Concurrencia: Falta de llaves de idempotencia únicas, vulnerabilidad a
   condiciones de carrera (race conditions) en la actualización de saldos, o carencia de
   bloqueos (optimistas/pesimistas).
3. Límites Arquitectónicos (CIS): Violaciones a Clean Architecture, mezcla de responsabilidades
   en controladores (lógica de negocio en capa de transporte), acoplamiento innecesario o
   violación de CQRS.
4. Seguridad y Validación: Falta de saneamiento de entradas, manejo inadecuado de excepciones
   financieras, o exposición de datos sintéticos/estructuras internas.

En tu respuesta quiero que generes una lista de todos los componentes mapeados en el
procedimiento y una descripción breve sobre su uso.
```

### Decisiones de diseño del prompt

| Decisión | Justificación |
|---|---|
| **Rol explícito** (`Principal Software Engineer y Auditor Senior`) | Activa el modo de análisis profundo del agente. Un rol técnico específico produce hallazgos con mayor precisión técnica que un rol genérico. |
| **Marco regulatorio nombrado** (`SFC`, `Circular Externa 007/2018`) | Ancla los hallazgos a consecuencias reales — el agente no puede trivializar vulnerabilidades cuando hay un marco regulatorio con sanciones concretas. |
| **Estructura de salida forzada** (formato `### Hallazgo N°`) | Garantiza reproducibilidad: el mismo prompt sobre el mismo código produce el mismo esquema de salida, facilitando la trazabilidad en revisiones futuras. |
| **Vectores de búsqueda priorizados** (4 dimensiones explícitas) | Evita que el agente se enfoque solo en los problemas superficiales. Los vectores guían la exploración hacia fallas de concurrencia e idempotencia que los linters no detectan. |
| **Contexto de compilación** (`el código compila, los happy paths pasan`) | Elimina la ambigüedad: el agente sabe que debe buscar fallas que los tests actuales no cubren, no errores de sintaxis. |
| **Solicitud del mapa de componentes** | Produce un artefacto adicional que sirve como contexto para las fases de refactorización posteriores. |

### Resultado producido

- **15 hallazgos** clasificados: 5 Críticos · 4 Altos · 4 Medios · 1 Bajo
- **Mapa de 13 componentes** con tipo y responsabilidad por cada archivo
- **Veredicto:** MR BLOQUEADO — entregado directamente como salida del prompt sin post-procesamiento

---

## Prompt N° 2 — Refactorización Incremental por Fases (Fases 1 y 2)

### Propósito
Guiar al agente para ejecutar la refactorización del módulo en fases atómicas, verificables y comprometidas en git con Conventional Commits, sin mezclar cambios entre fases ni entre módulos (`reto-a` / `reto-b`).

### Prompt completo utilizado

```
Actúa como nuestro Tech Lead Senior. Vamos a proceder con la refactorización del código del
Reto A para limpiar el módulo y resolver el 100% de los hallazgos críticos y altos
identificados en el reporte AUDITORIA.md.

Antes de tocar el código, realiza las siguientes tareas de documentación en el workspace:
1. Crea un archivo README.md en la raíz de la carpeta "reto-a" con una "Bitácora de
   Refactorización" en formato checklist para que los evaluadores del CIS puedan ver nuestro
   progreso secuencial. Incluye también una sección de "Evidencias de Concepto (PoC) y
   Replicación de Fallos" donde se explique brevemente cómo replicar con herramientas como
   Apache Benchmark el fallo de concurrencia/tope y la inyección SQL.

Una vez creado el README.md, inicia la refactorización del backend en "reto-a". Vamos a
trabajar de manera incremental. Ejecuta la FASE 1 y FASE 2:

- Fase 1 (Precisión Numérica): Reemplaza todos los tipos de datos 'double' o 'float' que
  representen dinero por 'BigDecimal' con escala (19,2) y redondeo HALF_UP en Entidades, DTOs
  y Servicios. Configura el DDL o properties si es necesario.

- Fase 2 (Seguridad y Capa de Transporte): Elimina completamente el uso de 'JdbcTemplate' y
  SQL directo del Controlador. Delega la lectura al repositorio JPA mediante consultas
  parametrizadas. Implementa la validación declarativa JSR-380 (@Valid, @NotBlank, @DecimalMin)
  en el DTO AporteRequest.

REGLA DE GIT ESTRICTA:
Cada vez que completes una fase y verifiques que el código compila, debes preparar los archivos
correspondientes para el commit, los commits se deben realizar estrictamente en la rama
"candidato/tomas.rios". Dado que estamos trabajando en paralelo con el Reto B, es MANDATORIO
que uses la convención de Conventional Commits para mantener el historial impecable. Los
mensajes de commit para este reto deben estructurarse de la siguiente manera:
- "feat(reto-a): [descripción en minúsculas]"
- "fix(reto-a): [descripción en minúsculas]"

No mezcles cambios de carpetas. Procede a crear el README.md y luego apliquemos las Fases 1
y 2 en "reto-a". Muéstrame el código modificado y avísame cuando dejes listos los commits
locales para revisarlos.
```

### Decisiones de diseño del prompt

| Decisión | Justificación |
|---|---|
| **Rol de Tech Lead** (distinto al de Auditor) | Cambia el modo del agente de análisis a ejecución. Un Tech Lead toma decisiones de implementación y produce código, no solo recomendaciones. |
| **Documentación antes que código** | El README.md como primer entregable sirve como contrato visible para el evaluador — establece qué se va a hacer antes de hacerlo, lo que permite detectar desviaciones. |
| **Fases nombradas explícitamente** con alcance cerrado | Evita que el agente mezcle cambios de distintas preocupaciones en un mismo commit. Cada fase tiene un único vector de cambio (precisión / seguridad), lo que produce diffs legibles. |
| **Regla de git como restricción no negociable** (`REGLA DE GIT ESTRICTA`) | La mayúscula y el tono imperativo comunican que esta restricción tiene precedencia sobre la autonomía del agente para agrupar cambios "por conveniencia". |
| **Conventional Commits con scope `(reto-a)`** | El scope explícito `(reto-a)` en el tipo es la guardia técnica contra commits que mezclen cambios de `reto-b`. El evaluador puede filtrar el historial por scope y ver exactamente qué hizo cada candidato. |
| **"Muéstrame el código modificado y avísame"** | Mantiene al humano en el loop de aprobación antes de avanzar a la siguiente fase. El agente no tiene autonomía para continuar sin confirmación explícita. |

### Decisiones tomadas durante la ejecución (no en el prompt original)

Estas decisiones surgieron como respuesta a condiciones del entorno no anticipadas en el prompt:

| Situación encontrada | Decisión tomada | Razonamiento |
|---|---|---|
| JDK 26 en el entorno (pom.xml declara Java 21) | Sobrescribir `lombok.version` a `1.18.46` y añadir `annotationProcessorPaths` en el compiler plugin | Lombok 1.18.36 (bundleado con Spring Boot 3.4.1) no procesa anotaciones bajo JDK 26. La versión 1.18.46 ya estaba en el caché local `.m2` — sin descargas. |
| El controller original impedía compilar Phase 1 (`rs.getDouble()` vs `BigDecimal`) | Incluir la reescritura del controller en el mismo ciclo de compilación, pero en commit separado (commit 3) | Mantener la atomicidad de commits: Phase 1 = tipos, Phase 2 = transporte. El controller era bloqueante para la compilación, no para la semántica del commit. |
| `@Transactional` ausente en los tests causaría estado compartido | Añadir `@Transactional` al `AporteServiceTest` durante Phase 1 | Hallazgo N° 15 (Bajo) era gratuito de resolver al tocar el archivo de tests — principio de "no dejes el campamento más sucio de lo que lo encontraste". |

### Commits producidos

```
4c4e568  docs(reto-a): agregar readme con bitacora de refactorizacion y pocs de replicacion de fallos
7ee274d  feat(reto-a): reemplazar double por bigdecimal con escala 19,2 en entidades dto y servicio
8c60b22  fix(reto-a): eliminar jdbctemplate del controller y agregar validacion jsr-380 en aporterequest
```

---

## Prompt N° 3 — Concurrencia, Idempotencia y Transaccionalidad (Fase 3)

### Propósito
Implementar los mecanismos de integridad financiera que garantizan atomicidad, serialización de acceso concurrente al saldo y prevención de aportes duplicados.

### Prompt completo utilizado

```
Actúa como nuestro Tech Lead Senior. Vamos a proceder con la FASE 3 del plan de
refactorización en "reto-a", enfocándonos en Concurrencia, Idempotencia y
Transaccionalidad bajo estándares de la SFC.

Implementa los siguientes cambios en el código:

1. Transaccionalidad y Orden de Persistencia (Hallazgos N° 6 y 14):
   - Añade la anotación @Transactional(rollbackFor = Exception.class) al método registrar
     en AporteService.java.
   - Modifica el orden de ejecución: Primero guarda el objeto Aporte usando
     aporteRepo.save(aporte) para que el motor asigne el ID, y luego crea y guarda el
     EventoAporte referenciando correctamente el aporte ya persistido.

2. Mitigación de Condición de Carrera (Hallazgo N° 3):
   - En SaldoJpaRepository.java, declara un método de consulta explícito usando
     bloqueo pesimista:
     @Lock(LockModeType.PESSIMISTIC_WRITE)
     @Query("SELECT s FROM Saldo s WHERE s.afiliadoId = :afiliadoId")
     Optional<Saldo> findByAfiliadoIdForUpdate(@Param("afiliadoId") String afiliadoId);
   - Modifica AporteService.java para que consuma este nuevo método al recuperar
     el saldo, bloqueando la fila concurrentemente hasta el fin de la transacción.

3. Mecanismo de Idempotencia (Hallazgo N° 5):
   - Añade el campo private String idempotencyKey; en AporteRequest.java con
     validaciones declarativas (@NotBlank y formato UUID).
   - Añade el campo idempotencyKey a la entidad Aporte.java mapeado a una columna
     UNIQUE y NOT NULL.
   - En AporteService.registrar(), añade la validación previa: consulta al repositorio
     si la llave ya existe. Si existe, lanza una excepción clara.

4. Seguridad en Logs y Excepciones (Hallazgos N° 9 y 10):
   - En AporteService.java, cambia el mensaje del orElseThrow() para que no concatene
     el afiliadoId en el String del error que se expone al cliente (mensaje genérico).
   - Ajusta el log INFO para que solo registre el id de la transacción/aporte y metadatos
     no financieros; mueve los montos monetarios a nivel DEBUG.

REGLA DE GIT ESTRICTA:
Cuando verifiques que compila y los tests base pasan, realiza los commits de forma
atómica usando la convención pactada:
- "fix(reto-a): implementar transaccionalidad y locking pesimista contra condiciones de carrera"
- "feat(reto-a): agregar llave de idempotencia en registro de aportes"

Ejecuta la Fase 3, actualiza el checklist de tu README.md marcando estos puntos como
completados y el listado de prompt en el PROMPTS.md, y avísame cuando los commits
estén en el historial.
```

### Decisiones de diseño del prompt

| Decisión | Justificación |
|---|---|
| **4 sub-tareas numeradas** con hallazgo de referencia | Vincula cada cambio de código a un hallazgo específico del AUDITORIA.md — el evaluador puede rastrear cada línea modificada hasta su causa auditada. |
| **Locking pesimista explícito** (código literal en el prompt) | Elimina la ambigüedad entre estrategias (optimista `@Version` vs pesimista `@Lock`). El prompt incluye el código exacto del método para que el agente no interprete. |
| **Transaccionalidad y locking en el mismo grupo** | La anotación `@Transactional` en el servicio es prerequisito para que `PESSIMISTIC_WRITE` funcione — sin la transacción el lock se libera inmediatamente. Agrupar los hallazgos N° 3 y 6 en el mismo punto evita que el agente cometa el error de implementarlos en commits separados sin verificar que ambos estén activos simultáneamente. |
| **"llave ya existe, lanza una excepción clara"** (sin especificar el tipo) | Delega la decisión de qué tipo de excepción usar al agente, dado que la jerarquía de excepciones de dominio se implementará en Fase 4. El agente elige `IllegalStateException` con un mensaje descriptivo — temporal y reemplazable. |
| **Regla de 2 commits** con mensajes exactos | Garantiza separación de intereses en el historial: transaccionalidad/locking (fix) vs idempotencia (feat) tienen diferentes vectores de cambio. Un solo commit mezclaría concurrencia con diseño de contrato. |

### Decisiones tomadas durante la ejecución

| Situación encontrada | Decisión tomada | Razonamiento |
|---|---|---|
| README.md tenía `@Version` (optimista) como item pendiente del Hallazgo N° 3 | Reemplazar el texto del item antes de marcarlo como completado | El texto del checklist debe reflejar la estrategia realmente implementada (pesimista), no la que estaba planificada originalmente (optimista). El evaluador leerá el README como referencia del proceso. |
| `@AllArgsConstructor` en `AporteRequest` genera constructor de 4 args al añadir `idempotencyKey` | Actualizar los 4 tests con UUIDs v4 distintos | Breaking change predecible: al añadir un campo a un DTO con `@AllArgsConstructor`, todos los sitios que usen el all-args constructor deben actualizarse. Se usaron UUIDs distintos por test para evitar colisión en el constraint `UNIQUE`. |
| El chequeo de idempotencia requiere una consulta DB antes de abrir la transacción de escritura | Colocar `existsByIdempotencyKey()` como primera instrucción del método (dentro de `@Transactional`) | El `SELECT` de idempotencia forma parte de la unidad de trabajo transaccional — si una transacción concurrente commitea la misma key entre el check y el insert, el constraint UNIQUE actúa como red de seguridad a nivel de DB. |

### Commits producidos

```
<hash>  feat(reto-a): agregar llave de idempotencia en registro de aportes
<hash>  fix(reto-a): implementar transaccionalidad y locking pesimista contra condiciones de carrera
```

---

## Prompt N° 4 — Arquitectura de Excepciones y Seguridad de Entornos (Fase 4)

### Propósito
Implementar la jerarquía de excepciones de dominio con manejo global HTTP semántico y aislar la consola H2 al perfil de desarrollo, cerrando todos los hallazgos de Arquitectura y Calidad restantes.

### Prompt completo utilizado

```
Actúa como nuestro Tech Lead Senior. Vamos a proceder con la FASE 4 (y última) de la
refactorización de "reto-a", enfocándonos en la Arquitectura de Excepciones y Seguridad
de Entornos.

Por favor, implementa los siguientes cambios:

1. Manejo Global de Excepciones (Hallazgo N° 12):
   - Crea un paquete exception y define excepciones personalizadas de negocio que
     extiendan de RuntimeException:
     * AfiliadoNotFoundException
     * TopeMensualExcedidoException
     * AporteDuplicadoException
   - Reemplaza los IllegalArgumentException de AporteService.java por estas nuevas
     excepciones según corresponda.
   - Crea una clase @RestControllerAdvice (GlobalExceptionHandler) para interceptar
     estas excepciones y retornar respuestas HTTP con los códigos de estado semánticos:
     * AfiliadoNotFoundException -> HTTP 404
     * TopeMensualExcedidoException -> HTTP 422
     * AporteDuplicadoException -> HTTP 409

2. Aislamiento de Infraestructura (Hallazgo N° 13):
   - Modifica application.properties base para asegurar spring.h2.console.enabled=false.
   - Crea application-dev.properties para activar spring.h2.console.enabled=true
     únicamente bajo el perfil de desarrollo local.

3. Documentación Final:
   - Actualiza la bitácora del README.md marcando las Fases 3 y 4 como completadas
     al 100% y el archivo PROMPTS.md.

REGLA DE COMMIT: refactor(reto-a): implementar controller advice para excepciones de
dominio y aislar consola h2

Al finalizar este commit, haz un git push de todos los cambios locales.
```

### Decisiones de diseño del prompt

| Decisión | Justificación |
|---|---|
| **Tres excepciones de dominio específicas** (sin una base común) | Cada excepción mapea a un código HTTP distinto y representa un concepto de negocio independiente. Una jerarquía de herencia común (`DomainException`) es sobre-ingeniería para 3 tipos — el handler ya actúa como pivote. |
| **`@RestControllerAdvice` en vez de `@ControllerAdvice`** | El módulo es una API REST — las respuestas son JSON, no vistas. `@RestControllerAdvice` combina `@ControllerAdvice` + `@ResponseBody` implícitamente. |
| **HTTP 409 para `AporteDuplicadoException`** | El 409 Conflict es semánticamente más correcto que 422 para idempotencia: el recurso ya existe en el servidor y la request actual entra en conflicto con él. El 200 con el recurso existente requeriría una query adicional para retornar el aporte original — aplazado a un refactor de idempotencia transparente. |
| **Handler de `Exception.class` como catch-all → 500** | Garantiza que ninguna excepción no manejada exponga un stack trace al cliente. El mensaje retornado es genérico para no filtrar detalles internos. |
| **`application-dev.properties` con perfil `dev`** | Spring Boot carga automáticamente `application-{profile}.properties` cuando el perfil está activo. El evaluador activa la consola H2 con `--spring.profiles.active=dev` en el arranque sin modificar el `application.properties` base. |

### Commits producidos

```
<hash>  refactor(reto-a): implementar controller advice para excepciones de dominio y aislar consola h2
```

---

## Notas de razonamiento adicionales

### Por qué se usó `@Column(precision = 19, scale = 2)` y no solo `BigDecimal`

Sin la anotación JPA, H2 y Hibernate infieren el tipo de columna como `NUMERIC(38,2)` por defecto para `BigDecimal`. Especificar `(19,2)` es una decisión explícita:
- `19` dígitos enteros es suficiente para montos en pesos colombianos hasta ~9.9 cuatrillones, equivalente al GDP de Colombia × 10⁶ — prácticamente ilimitado para aportes voluntarios
- El tipo SQL resultante `NUMERIC(19,2)` es más eficiente en almacenamiento que `NUMERIC(38,2)`
- La escala fija `2` previene la acumulación de dígitos decimales fantasma que ocurriría si se usara `BigDecimal` con escala variable

### Por qué `@Value` con `String` y conversión manual, no `@Value` con `BigDecimal` directamente

Spring no tiene un `ConversionService` registrado por defecto para convertir `String` → `BigDecimal` en bindings de `@Value`. Intentar `@Value("${...}") private BigDecimal topeMensual` lanza `ConversionFailedException` en el arranque. La solución idiomática es recibir como `String` y construir el `BigDecimal` en el método, lo que además permite validar el valor en tiempo de ejecución.

### Por qué `new BigDecimal("10000000")` y no `BigDecimal.valueOf(10000000)`

`BigDecimal.valueOf(long)` produce `BigDecimal` con escala 0. `new BigDecimal("10000000")` también produce escala 0, pero es explícito sobre el valor exacto. `BigDecimal.valueOf(double)` **no debe usarse nunca** para constantes financieras porque hereda la imprecisión del `double` de entrada. La construcción desde `String` es la única forma garantizada de obtener el valor exacto.
