# Hallazgos de Auditoría – Reto A

## Metodología utilizada

Se realizó una revisión manual del código fuente siguiendo los siguientes criterios:

* Seguridad de la aplicación (OWASP Top 10).
* Manejo de información financiera.
* Validaciones de negocio.
* Consistencia de datos.
* Buenas prácticas en Spring Boot y JPA.

Adicionalmente, se utilizó asistencia de IA como apoyo para complementar la revisión y contrastar posibles riesgos. Todos los hallazgos documentados fueron posteriormente verificados directamente en el código fuente.

**Prompt utilizado**

> Analiza un proyecto Spring Boot relacionado con aportes financieros e identifica posibles problemas de seguridad, manejo de dinero, validaciones de negocio, integridad de datos, concurrencia y consistencia transaccional. Explica el riesgo y la recomendación.

---

# Hallazgo 1: Posible SQL Injection

**Ubicación:** `AporteController.java` - método `consolidado()`

**Severidad:** Crítica

## Observación

En el controlador se construye una consulta SQL concatenando directamente parámetros recibidos desde la petición HTTP.

## Evidencia

```java
String sql = "SELECT * FROM aporte WHERE afiliado_id = '"
        + afiliadoId + "' AND periodo = '" + periodo + "'";
```

## Riesgo

Un usuario malicioso podría alterar los parámetros de entrada para modificar la consulta ejecutada y acceder a información no autorizada.

## Recomendación

Utilizar consultas parametrizadas o delegar la consulta a Spring Data JPA.

---

# Hallazgo 2: Uso de double para valores monetarios

**Ubicación:** `Aporte.java`, `Saldo.java`, `EventoAporte.java`, `AporteRequest.java`

**Severidad:** Alta

## Observación

Los montos financieros son almacenados utilizando el tipo de dato `double`.

## Evidencia

```java
private double monto;
private double totalMes;
```

## Riesgo

Los tipos de punto flotante pueden generar errores de precisión en operaciones financieras y acumulación de saldos.

## Recomendación

Utilizar `BigDecimal` para representar valores monetarios.

---

# Hallazgo 3: Validación insuficiente del tope mensual

**Ubicación:** `AporteService.java` - método `registrar()`

**Severidad:** Alta

## Observación

La validación del tope mensual únicamente se ejecuta cuando el valor acumulado es exactamente igual al límite configurado.

## Evidencia

```java
if (nuevo == topeMensual)
```

## Riesgo

Es posible registrar aportes que superen el límite permitido sin generar ninguna validación.

## Ejemplo

Si el tope es de 10.000.000 y el nuevo saldo queda en 10.000.001, la validación no se ejecuta.

## Recomendación

Validar utilizando una condición que contemple valores iguales o superiores al límite definido.

---

# Hallazgo 4: Consulta de saldo sin considerar el período

**Ubicación:** `SaldoJpaRepository.java`

**Severidad:** Media

## Observación

La búsqueda de saldo se realiza únicamente por afiliado.

## Evidencia

```java
Optional<Saldo> findByAfiliadoId(String afiliadoId);
```

La entidad `Saldo` almacena información asociada a un mes específico.

## Riesgo

Se podrían utilizar saldos correspondientes a períodos diferentes al que se está procesando.

## Recomendación

Consultar utilizando afiliado y período como criterios de búsqueda.

---

# Hallazgo 5: Ausencia de manejo transaccional

**Ubicación:** `AporteService.java` - método `registrar()`

**Severidad:** Alta

## Observación

El proceso de registro de aportes realiza múltiples operaciones de persistencia:

* Actualización de saldo.
* Registro de evento.
* Registro de aporte.

## Evidencia

Las operaciones se ejecutan de forma consecutiva sin manejo transaccional explícito.

## Riesgo

Si una operación falla después de haberse ejecutado otra, la información podría quedar inconsistente.

## Recomendación

Utilizar `@Transactional` para garantizar atomicidad y consistencia.

---

# Hallazgo 6: Riesgo de condición de carrera (Concurrencia)

**Ubicación:** `AporteService.java` - método `registrar()`

**Severidad:** Alta

## Observación

El saldo se consulta, modifica y guarda mediante un patrón de lectura-modificación-escritura sin mecanismos de sincronización o control de concurrencia.

## Evidencia

```java
Saldo s = saldoRepo.findByAfiliadoId(...);

double nuevo = s.getTotalMes() + monto;

s.setTotalMes(nuevo);

saldoRepo.save(s);
```

## Riesgo

Dos solicitudes concurrentes podrían leer el mismo saldo inicial y sobrescribir actualizaciones entre sí, generando pérdida de datos y valores incorrectos.

## Recomendación

Implementar control de concurrencia mediante transacciones apropiadas, bloqueo optimista/pesimista o mecanismos equivalentes soportados por JPA.
