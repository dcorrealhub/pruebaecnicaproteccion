# Auditoría de Código — Servicio de Aportes Voluntarios

**Módulo:** Registro de aportes a fondo voluntario  
**Stack:** Spring Boot 3.4.1 · Java 21 · H2 · Spring Data JPA  
**Revisado por:** Carolina Palacio Gutierrez  
**Fecha:** 19/06/2026

---

## Criterio de revisión

Este módulo manipula dinero en un contexto regulado (SFC). Los hallazgos se priorizan por impacto financiero, seguridad y corrección arquitectónica. Los marcados como **Críticos** bloquean la aprobación del MR.

---

## Resumen ejecutivo

| # | Hallazgo | Archivo | Severidad |
|---|---|---|------|
| H1 | `double` para representar dinero | Entidades | Crítica |
| H2 | Comparación de tope mensual con `==` | `AporteService` | Crítica |
| H3 | SQL Injection en `/consolidado` | `AporteController` | Crítica |
| H4 | Sin `@Transactional` en operación multitabla | `AporteService` | Crítica |
| H5 | Sin idempotencia — reintento duplica el aporte | `AporteService` | Crítica |
| H6 | Sin control de concurrencia en el saldo | `AporteService` / `Saldo` | Alta |
| H7 | Sin validaciones en el DTO de entrada | `AporteRequest` | Alta |
| H8 | SQL en el controlador — viola CQRS | `AporteController` | Alta |
| H9 | Entidad JPA expuesta como respuesta REST | `AporteController` | Alta |
| H10 | Consola H2 habilitada sin perfil de entorno | `application.properties` | Alta |
| H11 | Período derivado de `LocalDate.now()` | `AporteService` | Media |
| H12 | Sin manejo global de excepciones | (ausente) | Media |
| H13 | Logging insuficiente para auditoría regulatoria | `AporteService` | Media |
| H14 | `@Data` de Lombok en entidades JPA | Entidades | Media |

---

## Hallazgos Críticos — Bloquean el MR

### H1 — Uso de `double` para representar dinero

**Ubicación:** `Aporte.java` campo `monto` · `Saldo.java` campo `totalMes` · `EventoAporte.java` campo `monto`  
**Severidad:** Crítica

**Por qué es un problema:**  
`double` es un tipo de punto flotante binario que no puede representar exactamente la mayoría de valores decimales. En sumas acumuladas como el saldo mensual, el error de redondeo se propaga de forma impredecible. En un fondo regulado por la SFC, una diferencia de centavos entre lo registrado y lo real es un hallazgo de auditoría.

**Cómo corregirlo:**  
Reemplazar `double` por `BigDecimal` en todas las entidades, DTOs y lógica del servicio. Usar `BigDecimal.add()` para las sumas y definir la escala explícita al persistir.

---

### H2 — Validación del tope mensual con `==` en lugar de `>`

**Ubicación:** `AporteService.java` · validación del tope mensual  
**Severidad:** Crítica

**Por qué es un problema:**  
La condición actual compara si el nuevo saldo es exactamente igual al tope. Con `double`, esa igualdad exacta casi nunca se cumple por errores de redondeo, por lo que un monto que supere el tope pasa sin ser bloqueado. Es un bug directo en una regla de negocio regulada.

**Cómo corregirlo:**
```java
// Antes
if (nuevo == topeMensual)

// Después (con BigDecimal)
if (nuevo.compareTo(topeMensual) > 0)
```

---

### H3 — SQL Injection en el endpoint GET `/consolidado`

**Ubicación:** `AporteController.java` · método `consolidado()` · líneas ~41-42  
**Severidad:** Crítica

**Por qué es un problema:**  
Los parámetros `afiliadoId` y `periodo` se concatenan directamente en la cadena SQL sin escape ni parametrización. Permite acceder a datos de todos los afiliados. OWASP A03:2021 — Injection.

**Ejemplo de explotación:**
```
GET /api/aportes/consolidado?afiliadoId=AF-001' OR '1'='1&periodo=2026-06
```

**Cómo corregirlo:**
```java
// Reemplazar el bloque JdbcTemplate por:
return aporteJpaRepository.findByAfiliadoIdAndPeriodo(afiliadoId, periodo);
```

---

### H4 — Ausencia de `@Transactional` en operación que toca 3 tablas

**Ubicación:** `AporteService.java` · método `registrar()`  
**Severidad:** Crítica

**Por qué es un problema:**  
El método actualiza `Saldo`, persiste `Aporte` y persiste `EventoAporte` sin envoltura transaccional. Si ocurre un error intermedio, el saldo queda incrementado sin aporte registrado. Inconsistencia irrecuperable en datos financieros.

**Cómo corregirlo:**
```java
@Transactional
public Aporte registrar(AporteRequest req) {
    // ...
}
```

---

### H5 — Sin idempotencia: un reintento duplica el aporte

**Ubicación:** `AporteService.java` · método `registrar()` — no existe verificación de aporte previo  
**Severidad:** Crítica

**Por qué es un problema:**  
Un reintento por timeout de red o doble clic registra el mismo aporte dos veces, incrementa el saldo dos veces y genera dos eventos de auditoría. Error financiero real y auditable ante la SFC.

**Cómo corregirlo:**
```java
// En AporteRequest
private String requestId; // UUID generado por el cliente

// En AporteService.registrar()
if (aporteRepository.existsByRequestId(req.getRequestId())) {
    return aporteRepository.findByRequestId(req.getRequestId());
}
```

---

## Hallazgos Altos — Requieren justificación explícita para aprobar

### H6 — Sin control de concurrencia en la actualización del saldo

**Ubicación:** `AporteService.java` · método `registrar()` · `Saldo.java`  
**Severidad:** Alta

**Por qué es un problema:**  
Dos solicitudes simultáneas para el mismo afiliado leen el mismo saldo inicial y el segundo en persistir sobrescribe al primero. Uno de los aportes se pierde silenciosamente sin error visible.

**Cómo corregirlo:**
```java
// En Saldo.java — activa optimistic locking
@Version
private Long version;
```
Alternativa: `@Lock(LockModeType.PESSIMISTIC_WRITE)` en la query del repositorio.

---

### H7 — Sin validaciones en el DTO de entrada

**Ubicación:** `AporteRequest.java` — sin anotaciones de Bean Validation  
**Severidad:** Alta

**Por qué es un problema:**  
`monto` puede llegar null o negativo, `afiliadoId` puede estar vacío. El servicio hace validación manual parcial y una NPE puede ocurrir antes de detectarlo.

**Cómo corregirlo:**
```java
// AporteRequest.java
@NotBlank
private String afiliadoId;

@NotNull @Positive
private BigDecimal monto;

@NotBlank
private String canal;

// AporteController.java
public ResponseEntity<?> registrar(@Valid @RequestBody AporteRequest req) {
```

---

### H8 — Violación de CQRS: consulta SQL directa en el controlador

**Ubicación:** `AporteController.java` · método `consolidado()`  
**Severidad:** Alta

**Por qué es un problema:**  
El controlador ejecuta SQL con `JdbcTemplate`, mezclando presentación con acceso a infraestructura. Viola CQRS y Clean Architecture — la capa de presentación no debe conocer detalles de persistencia.

**Cómo corregirlo:**
```java
// Nueva clase: AporteQueryService.java
public List<AporteResponse> consultarConsolidado(String afiliadoId, String periodo) {
    return aporteRepository.findByAfiliadoIdAndPeriodo(afiliadoId, periodo)
        .stream()
        .map(AporteResponse::from)
        .toList();
}
```

---

### H9 — Entidad JPA expuesta directamente como respuesta REST

**Ubicación:** `AporteController.java` · método `registrar()` — retorna `Aporte` directamente  
**Severidad:** Alta

**Por qué es un problema:**  
Acopla el modelo de persistencia al contrato de la API. Cualquier cambio en la entidad rompe el contrato con clientes. Se exponen campos internos como `marcadaRevision` que no deben ser visibles.

**Cómo corregirlo:**
```java
public record AporteResponse(String afiliadoId, BigDecimal monto, String periodo, String canal) {
    public static AporteResponse from(Aporte a) {
        return new AporteResponse(a.getAfiliadoId(), a.getMonto(), a.getPeriodo(), a.getCanal());
    }
}
```

---

### H10 — Consola H2 habilitada sin perfil de entorno

**Ubicación:** `application.properties` — `spring.h2.console.enabled=true`  
**Severidad:** Alta

**Por qué es un problema:**  
Si esta configuración llega a QA, expone acceso directo a la base de datos desde `/h2-console`. Las credenciales son `sa` sin contraseña, lo que agrava el riesgo.

**Cómo corregirlo:**
```properties
# application.properties
spring.h2.console.enabled=false

# application-dev.properties
spring.h2.console.enabled=true
```

---

## Hallazgos Medios — Deben quedar en backlog documentado

### H11 — Período derivado de `LocalDate.now()` en el servidor

**Ubicación:** `AporteService.java` · cálculo del campo `periodo`  
**Severidad:** Media

**Por qué es un problema:**  
Aportes retroactivos o correcciones quedarán con el período del día de procesamiento, no del período real. Además imposibilita pruebas deterministas.

**Cómo corregirlo:**
```java
@NotBlank
@Pattern(regexp = "\\d{4}-\\d{2}")
private String periodo; // formato YYYY-MM en AporteRequest
```

---

### H12 — Sin manejo global de excepciones

**Ubicación:** Ausencia de `@RestControllerAdvice` en todo el proyecto  
**Severidad:** Media

**Por qué es un problema:**  
Las excepciones del servicio llegan al cliente como error 500 con stack trace expuesto. Los códigos HTTP son incorrectos — un monto inválido debería retornar 400, un afiliado no encontrado debería retornar 404.

**Cómo corregirlo:**
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleValidacion(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleEstado(IllegalStateException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}
```

---

### H13 — Logging insuficiente para auditoría regulatoria

**Ubicación:** `AporteService.java` — un único `log.info` en todo el flujo  
**Severidad:**  Media

**Por qué es un problema:**  
En un fondo regulado por la SFC se requiere trazabilidad completa por operación. El `EventoAporte` en base de datos no es suficiente si el sistema de observabilidad opera sobre logs estructurados.

**Cómo corregirlo:**
```java
MDC.put("afiliadoId", req.getAfiliadoId());
log.info("aporte.iniciado periodo={} monto={}", req.getPeriodo(), req.getMonto());
// ... lógica ...
log.info("aporte.completado marcadaRevision={}", aporte.isMarcadaRevision());
MDC.clear();
```

---

### H14 — `@Data` de Lombok en entidades JPA

**Ubicación:** `Aporte.java`, `Saldo.java`, `EventoAporte.java`  
**Severidad:** Media

**Por qué es un problema:**  
`@Data` genera `equals` y `hashCode` sobre todos los campos incluyendo `id`. En entidades con `id` autogenerado, una instancia antes y después de persistir no es igual a sí misma, rompiendo colecciones y el caché de Hibernate.

**Cómo corregirlo:**
```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Aporte { ... }
```

---

## Escenarios no cubiertos por los tests

| Escenario de riesgo | Cubierto |
|---|----|
| Monto exactamente igual al tope mensual | NO |
| Monto superior al tope mensual | NO (el bug H2 pasaría sin error) |
| Dos aportes simultáneos del mismo afiliado | No |
| Aporte duplicado por reintento de red | NO |
| `afiliadoId` con caracteres especiales o SQL | NO |
| `monto` null en el request | NO |
