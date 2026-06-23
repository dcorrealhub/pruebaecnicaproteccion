

# IMPORTANTE!!
## Hola Didier, resulta que ya habia enviado los hallazgos obtenidos, sin embargo sentí que lo podia mejorar juntandolo con los prompts, sin embargo cometi un error y envie los prompts sin los hallazgos, pero lo puedes evidenciar en el github, en el primer commit tengo los hallazgos y en el segundo los prompts, ambos dentro del plazo establecido.Pero aca te mando este que esta completo.

# Hallazgos de Auditoría – Reto A

## Metodología utilizada

Se realizó una revisión manual del código fuente siguiendo los siguientes criterios:

* Seguridad de la aplicación (OWASP Top 10).
* Manejo de información financiera.
* Validaciones de negocio.
* Consistencia de datos.
* Buenas prácticas en Spring Boot y JPA.

Adicionalmente, se utilizó asistencia de IA como apoyo para complementar la revisión y contrastar posibles riesgos. Todos los hallazgos documentados fueron posteriormente verificados manualmente en el código fuente antes de ser incluidos en el informe final.

---

# Proceso de Auditoría

## Objetivo

Realizar una revisión del código como si se tratara de un Merge Request dentro de un entorno financiero regulado, priorizando riesgos que pudieran afectar:

* Seguridad.
* Integridad de la información.
* Correctitud de cálculos financieros.
* Consistencia transaccional.
* Concurrencia.
* Cumplimiento de reglas de negocio.

---

# Prompts utilizados durante la auditoría

## 1. Revisión general del proyecto

> Analiza un proyecto Spring Boot relacionado con aportes financieros e identifica posibles problemas de seguridad, manejo de dinero, validaciones de negocio, integridad de datos, concurrencia y consistencia transaccional. Explica el riesgo y la recomendación.

### Resultado obtenido

Se identificaron posibles líneas de revisión relacionadas con:

* Validaciones de negocio.
* Manejo de montos monetarios.
* Persistencia de información.
* Concurrencia.
* Seguridad de acceso a datos.

Estos hallazgos potenciales fueron utilizados como guía inicial para la inspección manual.

---

## 2. Auditoría orientada a entorno financiero

> Analiza el código como si estuvieras revisando un Merge Request para una entidad financiera. Prioriza problemas relacionados con precisión numérica, manejo de dinero, consistencia transaccional, concurrencia, idempotencia y seguridad. Clasifica los hallazgos por severidad.

### Resultado obtenido

Se identificó la necesidad de revisar especialmente:

* Uso de tipos numéricos para dinero.
* Validaciones de topes financieros.
* Persistencia consistente de aportes y saldos.
* Posibles condiciones de carrera.
* Controles de idempotencia.

---

## 3. Búsqueda de vulnerabilidades de seguridad

> Revisa el código buscando posibles vulnerabilidades OWASP Top 10. Identifica riesgos de SQL Injection, validación insuficiente de entradas, exposición de información sensible o cualquier patrón inseguro. Explica el impacto y la mitigación recomendada.

### Resultado obtenido

Se identificó una posible construcción de consultas mediante concatenación de parámetros, lo que motivó una revisión específica del controlador y acceso a datos.

---

## 4. Revisión de manejo de dinero

> Analiza el uso de tipos numéricos dentro del proyecto. Identifica posibles riesgos asociados al uso de double, float o comparaciones numéricas incorrectas en procesos financieros. Explica las mejores prácticas recomendadas.

### Resultado obtenido

Se detectó la necesidad de revisar:

* Uso de double en montos financieros.
* Comparaciones de valores monetarios.
* Posibles errores de precisión acumulada.

---

## 5. Revisión de concurrencia y consistencia

> Analiza los procesos de actualización de saldo y persistencia de aportes. Identifica posibles condiciones de carrera, pérdida de actualizaciones o inconsistencias provocadas por accesos concurrentes. Explica alternativas de mitigación utilizando capacidades de JPA y Spring.

### Resultado obtenido

Se identificó un patrón lectura-modificación-escritura susceptible a pérdida de actualizaciones concurrentes.

---

## 6. Revisión de transacciones

> Identifica operaciones que deberían ejecutarse dentro de una misma transacción. Explica qué inconsistencias podrían ocurrir si una operación falla después de haberse persistido parcialmente el estado.

### Resultado obtenido

Se identificó que el proceso de registro de aportes involucraba múltiples operaciones de persistencia que deberían ejecutarse de forma atómica.

---

# Estrategia utilizada

Durante la auditoría se utilizó el siguiente enfoque:

**Analizar → Contrastar → Verificar → Documentar**

La IA fue utilizada para sugerir posibles riesgos y líneas de análisis.

Ningún hallazgo fue documentado sin ser verificado posteriormente en el código fuente.

La clasificación de severidad, la evidencia presentada y las recomendaciones propuestas fueron definidas después de revisar manualmente cada caso identificado.

---

# Hallazgos Encontrados

## Hallazgo 1: Posible SQL Injection

**Ubicación:** `AporteController.java` - método `consolidado()`

**Severidad:** Crítica

### Observación

En el controlador se construye una consulta SQL concatenando directamente parámetros recibidos desde la petición HTTP.

### Evidencia

```java
String sql = "SELECT * FROM aporte WHERE afiliado_id = '"
        + afiliadoId + "' AND periodo = '" + periodo + "'";
```

### Riesgo

Un usuario malicioso podría alterar los parámetros de entrada para modificar la consulta ejecutada y acceder a información no autorizada.

### Recomendación

Utilizar consultas parametrizadas o delegar la consulta a Spring Data JPA.

---

## Hallazgo 2: Uso de double para valores monetarios

**Ubicación:** `Aporte.java`, `Saldo.java`, `EventoAporte.java`, `AporteRequest.java`

**Severidad:** Alta

### Observación

Los montos financieros son almacenados utilizando el tipo de dato `double`.

### Evidencia

```java
private double monto;
private double totalMes;
```

### Riesgo

Los tipos de punto flotante pueden generar errores de precisión en operaciones financieras y acumulación de saldos.

### Recomendación

Utilizar `BigDecimal` para representar valores monetarios.

---

## Hallazgo 3: Validación insuficiente del tope mensual

**Ubicación:** `AporteService.java` - método `registrar()`

**Severidad:** Alta

### Observación

La validación del tope mensual únicamente se ejecuta cuando el valor acumulado es exactamente igual al límite configurado.

### Evidencia

```java
if (nuevo == topeMensual)
```

### Riesgo

Es posible registrar aportes que superen el límite permitido sin generar ninguna validación.

### Ejemplo

Si el tope es de 10.000.000 y el nuevo saldo queda en 10.000.001, la validación no se ejecuta.

### Recomendación

Validar utilizando una condición que contemple valores iguales o superiores al límite definido.

---

## Hallazgo 4: Consulta de saldo sin considerar el período

**Ubicación:** `SaldoJpaRepository.java`

**Severidad:** Media

### Observación

La búsqueda de saldo se realiza únicamente por afiliado.

### Evidencia

```java
Optional<Saldo> findByAfiliadoId(String afiliadoId);
```

La entidad `Saldo` almacena información asociada a un mes específico.

### Riesgo

Se podrían utilizar saldos correspondientes a períodos diferentes al que se está procesando.

### Recomendación

Consultar utilizando afiliado y período como criterios de búsqueda.

---

## Hallazgo 5: Ausencia de manejo transaccional

**Ubicación:** `AporteService.java` - método `registrar()`

**Severidad:** Alta

### Observación

El proceso de registro de aportes realiza múltiples operaciones de persistencia:

* Actualización de saldo.
* Registro de evento.
* Registro de aporte.

### Evidencia

Las operaciones se ejecutan de forma consecutiva sin manejo transaccional explícito.

### Riesgo

Si una operación falla después de haberse ejecutado otra, la información podría quedar inconsistente.

### Recomendación

Utilizar `@Transactional` para garantizar atomicidad y consistencia.

---

## Hallazgo 6: Riesgo de condición de carrera (Concurrencia)

**Ubicación:** `AporteService.java` - método `registrar()`

**Severidad:** Alta

### Observación

El saldo se consulta, modifica y guarda mediante un patrón de lectura-modificación-escritura sin mecanismos de sincronización o control de concurrencia.

### Evidencia

```java
Saldo s = saldoRepo.findByAfiliadoId(...);

double nuevo = s.getTotalMes() + monto;

s.setTotalMes(nuevo);

saldoRepo.save(s);
```

### Riesgo

Dos solicitudes concurrentes podrían leer el mismo saldo inicial y sobrescribir actualizaciones entre sí, generando pérdida de datos y valores incorrectos.

### Recomendación

Implementar control de concurrencia mediante transacciones apropiadas, bloqueo optimista/pesimista o mecanismos equivalentes soportados por JPA.
