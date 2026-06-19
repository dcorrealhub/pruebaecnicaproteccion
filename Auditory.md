# Auditoría de código — `reto-a`

> **Estado:** cerrada. Todos los archivos del módulo fueron revisados (`controller`, `service`, `domain`, `dto`, `repository`, `test`).

Servicio de registro de aportes a un fondo voluntario. Compilaba y los tests "felices" pasaban, pero el módulo tenía hallazgos que bloquearían un MR en cualquier célula con revisión seria de seguridad y manejo de dinero. Este documento detalla cada hallazgo, su severidad, el código que lo origina y el fix aplicado.

---

## Resumen de hallazgos

| # | Hallazgo | Severidad | Archivo | Estado |
|---|----------|-----------|---------|--------|
| 1 | SQL Injection en `/consolidado` | **Crítico** | `AporteController.java` | ✅ Corregido |
| 2 | `double` para representar dinero | **Crítico** | `Aporte`, `EventoAporte`, `Saldo`, `AporteRequest` | ✅ Corregido |
| 3 | Comparación `==` rompe el control de tope mensual | **Crítico** | `AporteService.java` | ✅ Corregido |
| 4 | Condición de carrera (*lost update*) en `Saldo` | **Alto** | `AporteService.java` | ✅ Corregido |
| 5 | Sin validación de entrada en `AporteRequest` | **Alto** | `AporteRequest.java` | ✅ Corregido |
| 6 | Acceso directo a JDBC desde el controller, bypaseando la capa de servicio | **Medio** | `AporteController.java` | ✅ Corregido |
| 7 | Sin manejo global de excepciones (`IllegalArgumentException` → 500 genérico) | **Medio** | (transversal) | ⚠️ Pendiente |
| 8 | `@Data` de Lombok en entidades JPA | **Bajo** | `Aporte`, `EventoAporte`, `Saldo` | ✅ Corregido |
| 9 | `marcadaRevision` — confirmado, sí tiene uso de negocio | **Informativo** | `Aporte.java` | ✔️ Cerrado, sin acción |
| 10 | Tests usaban literales `double`, no cubrían condición de carrera ni el bug del `==` | **Medio** | `AporteServiceTest.java` | ✅ Corregido (parcial, ver pendientes) |

---

## 1. SQL Injection en `GET /api/aportes/consolidado` — **CRÍTICO**

**Ubicación:** `AporteController.java`, método `consolidado`.

```java
String sql = "SELECT * FROM aporte WHERE afiliado_id = '"
        + afiliadoId + "' AND periodo = '" + periodo + "'";
return jdbc.query(sql, aporteRowMapper);
```

Los parámetros `afiliadoId` y `periodo` llegan desde un `@RequestParam` (entrada de usuario sin sanitizar) y se concatenan directamente dentro de la consulta SQL.

**Ejemplo de explotación:**

```
GET /api/aportes/consolidado?afiliadoId=AF-001' OR '1'='1&periodo=2025-06
```

Esto devuelve **todos** los aportes de **todos** los afiliados, no solo los de `AF-001` — fuga de datos financieros de terceros. Con un motor distinto a H2 en memoria (ej. Postgres en producción), el vector se extiende a exfiltración de cualquier tabla, denegación de servicio, o modificación/borrado de datos según permisos del usuario de BD.

**Por qué pasó:** ya existía `AporteJpaRepository.findByAfiliadoIdAndPeriodo`, parametrizado por diseño, pero el controller lo ignoraba y construía SQL crudo a mano con un `JdbcTemplate` inyectado en paralelo.

**Fix aplicado:** se eliminó el `JdbcTemplate` y el `RowMapper` manual del controller. El endpoint ahora delega en el `service`, que usa el repositorio JPA:

```java
@GetMapping("/consolidado")
public List<Aporte> consolidado(@RequestParam String afiliadoId,
                                 @RequestParam String periodo) {
    return service.consolidado(afiliadoId, periodo);
}
```

```java
// AporteService.java
public List<Aporte> consolidado(String afiliadoId, String periodo) {
    return aporteRepo.findByAfiliadoIdAndPeriodo(afiliadoId, periodo);
}
```

---

## 2. Uso de `double` para representar dinero — **CRÍTICO**

**Ubicación:** `Aporte.monto`, `EventoAporte.monto`, `Saldo.totalMes`, `AporteRequest.monto`, y los `@Value` de tope/umbral en `AporteService`.

`double` es punto flotante binario (IEEE 754) y no puede representar exactamente la mayoría de cantidades decimales:

```java
double a = 0.1;
double b = 0.2;
System.out.println(a + b); // 0.30000000000000004, no 0.3
```

Sumar múltiples aportes mensuales sobre `Saldo.totalMes` puede generar diferencias de centavos que no concilian contra el sistema contable.

**Fix aplicado:** migración completa a `BigDecimal` con escala fija en entidades, DTO, y los parámetros de configuración (`topeMensual`, `umbralRevision`):

```java
@Column(precision = 19, scale = 2)
private BigDecimal monto;
```

```java
@Value("${aporte.tope-mensual:10000000}")
private BigDecimal topeMensual;
```

Toda la aritmética en `AporteService` se reescribió usando los métodos de `BigDecimal` (`.add()`, `.compareTo()`) en vez de operadores (`+`, `==`, `>`), que no aplican a este tipo.

---

## 3. Comparación `==` rompe el control de tope mensual — **CRÍTICO**

**Ubicación:** `AporteService.java`, método `registrar` (código original).

```java
double nuevo = s.getTotalMes() + monto;

if (nuevo == topeMensual) {
    throw new IllegalArgumentException("El monto supera el tope mensual permitido");
}
```

Este es el hallazgo más grave de lógica de negocio del módulo, y no era evidente sin leer `AporteService` completo. El control de tope mensual **no funcionaba en absoluto** para el caso real que debía prevenir:

- La condición usa **igualdad exacta** (`==`) en vez de "mayor o igual que" (`>=`). Si el acumulado supera el tope —por ejemplo `10,000,001` contra un tope de `10,000,000`— la condición es `false` y el aporte se registra sin objeción.
- Solo se bloquea el caso en que el acumulado coincide **exactamente, centavo a centavo**, con el tope configurado — un escenario casi imposible en la práctica dado que los aportes son montos variables.
- Adicionalmente, comparar `double` con `==` es frágil por errores de redondeo de punto flotante, incluso si la lógica de negocio hubiera sido correcta.

**Impacto real:** un afiliado podía superar el tope mensual permitido por el fondo sin que el sistema lo impidiera, ya que la validación nunca se disparaba en la práctica.

**Fix aplicado:**

```java
BigDecimal nuevo = s.getTotalMes().add(monto);

if (nuevo.compareTo(topeMensual) > 0) {
    throw new IllegalArgumentException("El monto supera el tope mensual permitido");
}
```

`compareTo(...) > 0` bloquea correctamente cualquier acumulado que **exceda** el tope, sea por un centavo o por una cantidad mayor — que es la regla de negocio real.

---

## 4. Condición de carrera (*lost update*) en `Saldo` — **ALTO**

**Ubicación:** `AporteService.java`, método `registrar` (código original).

```java
Saldo s = saldoRepo.findByAfiliadoId(req.getAfiliadoId()).orElseThrow(...);
double nuevo = s.getTotalMes() + monto;
s.setTotalMes(nuevo);
saldoRepo.save(s);
```

No había `@Transactional` ni ningún mecanismo de locking. Es un patrón clásico de *read-then-write* sin control de concurrencia. Con dos solicitudes simultáneas de aporte para el mismo afiliado:

1. Ambas leen `s.getTotalMes()` con el mismo valor base (ej. `1,000,000`).
2. Cada una suma su propio `monto` sobre esa misma base.
3. La que escribe último sobrescribe a la otra — un aporte completo se pierde del acumulado de `Saldo`, aunque ambos aportes individuales quedan guardados como filas en `Aporte` (esto generaría una inconsistencia detectable entre el detalle y el consolidado, pero solo después del hecho).

Esto también socavaba aún más el control de tope mensual (ya roto por el hallazgo #3): bajo concurrencia, el chequeo se hacía sobre un valor de `totalMes` potencialmente desactualizado.

**Fix aplicado:** se agregó `@Transactional` al método `registrar`, y un lock pesimista a nivel de fila sobre `Saldo` durante la transacción:

```java
@Transactional
public Aporte registrar(AporteRequest req) {
    // ...
    Saldo s = saldoRepo.findByAfiliadoIdForUpdate(req.getAfiliadoId())
            .orElseThrow(...);
    // ...
}
```

```java
// SaldoJpaRepository.java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT s FROM Saldo s WHERE s.afiliadoId = :afiliadoId")
Optional<Saldo> findByAfiliadoIdForUpdate(@Param("afiliadoId") String afiliadoId);
```

Se eligió lock pesimista en vez de `@Version` (optimistic locking) porque cada registro de aporte necesita el valor *actual* del acumulado para sumar correctamente; un conflicto optimista obligaría a reintentar la transacción completa. Con un lock pesimista, las transacciones concurrentes para el mismo afiliado se serializan de forma simple y predecible. Si el volumen de aportes concurrentes por afiliado creciera significativamente, valdría la pena revisitar con `@Version` + retry para reducir contención.

---

## 5. Sin validación de entrada en `AporteRequest` — **ALTO**

**Ubicación:** `AporteRequest.java` (código original) y `AporteController.registrar`.

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AporteRequest {
    private String afiliadoId;
    private double monto;
    private String canal;
}
```

Sin anotaciones de validación ni `@Valid` en el controller, era posible enviar `monto` negativo o cero (aunque el `service` sí validaba esto en código, no había defensa en la capa HTTP), `afiliadoId` nulo/vacío, o `canal` con cualquier valor arbitrario.

**Fix aplicado:**

```java
public class AporteRequest {

    @NotBlank(message = "afiliadoId es obligatorio")
    private String afiliadoId;

    @NotNull(message = "monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    private BigDecimal monto;

    @NotBlank(message = "canal es obligatorio")
    private String canal;
}
```

```java
@PostMapping
public Aporte registrar(@Valid @RequestBody AporteRequest req) {
    return service.registrar(req);
}
```

> **Nota:** `canal` se deja como `String` con `@NotBlank` en vez de migrarlo a un `enum` (`CanalAporte`) porque no se confirmó con el negocio cuál es el conjunto cerrado de valores válidos (`APP_MOVIL`, `WEB`, ¿otros?). Recomiendo cerrar este punto en un siguiente MR una vez se confirme la lista completa de canales soportados — ver sección de pendientes.

---

## 6. Acceso directo a JDBC desde el controller — **MEDIO**

**Ubicación:** `AporteController.java`.

El controller inyectaba `JdbcTemplate` y construía su propio `RowMapper`, sin pasar por la capa de servicio ni el repositorio JPA — violación de separación de incumbencias (SoC) y duplicación de responsabilidad, dado que `AporteJpaRepository.findByAfiliadoIdAndPeriodo` ya cubría el mismo caso de forma segura.

**Fix aplicado:** resuelto como parte del fix de la sección 1 — se eliminó `JdbcTemplate` y `RowMapper` del controller por completo.

---

## 7. Sin manejo global de excepciones — **MEDIO** ⚠️ Pendiente

**Ubicación:** transversal (no hay `@ExceptionHandler` ni `@ControllerAdvice` en el módulo).

`AporteService.registrar` lanza `IllegalArgumentException` para los casos de monto inválido, afiliado inexistente, y tope mensual excedido. Sin un manejador global, Spring devuelve un **500 Internal Server Error** genérico para estos casos, cuando semánticamente corresponden a un **400 Bad Request** (entrada inválida) o **404 Not Found** (afiliado inexistente).

Esto es un problema real para cualquier cliente del API: no puede distinguir "el servidor falló" de "tu request fue inválido", y expone trazas de stack internas si `server.error.include-stacktrace` no está deshabilitado explícitamente en `application.properties`.

**Fix recomendado (no aplicado aún):**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(Map.of("error", detalle));
    }
}
```

Queda pendiente porque requiere decidir junto al equipo el contrato de error exacto (formato de respuesta, si se distingue 400 de 404 para afiliado inexistente, etc.) — no es una corrección puramente mecánica como las anteriores.

---

## 8. `@Data` de Lombok en entidades JPA — **BAJO**

**Ubicación:** `Aporte.java`, `EventoAporte.java`, `Saldo.java`.

`@Data` genera `equals()`/`hashCode()` sobre todos los campos, lo cual es frágil en entidades JPA: la identidad de un objeto "cambia" cuando pasa de `id = null` a `id` asignado tras persistir, y puede disparar carga perezosa si se agregan relaciones en el futuro.

**Fix aplicado:** se reemplazó `@Data` por `@Getter` + `@Setter` explícitos en las tres entidades.

---

## 9. `marcadaRevision` — confirmado con uso real

**Ubicación:** `Aporte.java`, `AporteService.java`.

```java
aporte.setMarcadaRevision(monto.compareTo(umbralRevision) > 0);
```

Confirmado: no es código muerto. Marca para revisión manual los aportes que superan un umbral de cumplimiento (`umbralRevision`, configurable, default `5,000,000`). Cerrado sin acción.

---

## 10. Tests con literales `double` y cobertura incompleta — **MEDIO**

**Ubicación:** `AporteServiceTest.java` (código original).

```java
var req = new AporteRequest("AF-001", 500_000.0, "APP_MOVIL");
```

Tras migrar `AporteRequest.monto` a `BigDecimal`, estos literales `double` rompían la compilación del test. Más allá de eso, la suite original solo cubría 4 casos felices/de validación básica (monto válido, umbral de revisión, monto negativo, afiliado inexistente) — **no existía ningún test que cubriera el bug del tope mensual (#3) ni la condición de carrera (#4)**, los dos hallazgos más graves de lógica de negocio del módulo.

**Fix aplicado (parcial):** se migraron los 4 tests existentes a `BigDecimal`. **Pendiente** (ver sección final): agregar test de tope mensual excedido y test de concurrencia.

---

## Pendientes para un siguiente MR

- [ ] **Manejo global de excepciones** (`@RestControllerAdvice`) — hallazgo #7, requiere alinear contrato de error con el equipo.
- [ ] **Test de tope mensual:** verificar que un aporte que haga que `totalMes` *supere* (no solo iguale) el tope sea rechazado — cubre directamente la regresión del bug `==` (#3).
- [ ] **Test de concurrencia:** dos hilos registrando aportes simultáneos para el mismo afiliado; verificar que `Saldo.totalMes` refleje la suma de ambos sin pérdidas — cubre el fix de la sección 4.
- [ ] **Definir enum `CanalAporte`** una vez se confirme con negocio el conjunto cerrado de canales válidos (ver nota en hallazgo #5).
- [ ] **Revisar `application.properties` y `data.sql`:** confirmar que la consola H2 (`/h2-console`) no quede expuesta en un perfil de producción, y que no haya credenciales hardcodeadas.

---

## Metodología de revisión

Esta auditoría se hizo leyendo el código real, capa por capa, sin asumir comportamiento no confirmado por el archivo fuente:

1. **Dominio primero** (`Aporte`, `Saldo`, `EventoAporte`): para entender el modelo de datos y detectar de entrada el uso de `double` para dinero.
2. **Controller y DTO**: para mapear los puntos de entrada del sistema (dónde llega input no confiable) y detectar el SQL injection.
3. **Service al final**: porque ahí vive la lógica de negocio real (reglas de tope, concurrencia, efectos secundarios), y es el archivo donde un hallazgo "se confirma o se descarta" — varios hallazgos de este documento quedaron explícitamente marcados como *pendientes de confirmar* hasta tener ese archivo, en vez de especular sobre código no visto.
4. **Tests al cierre**: para verificar si la suite existente realmente protege contra los hallazgos encontrados, o si solo cubre el camino feliz (resultó ser lo segundo: cero cobertura sobre los dos bugs de negocio más graves).

Cada fix propuesto en este documento fue validado contra el código real antes de entregarse, y los cambios se aplicaron de forma incremental verificando que cada capa siguiera compilando contra la anterior (entidad → DTO → service → repositorio → test), en vez de reescribir todo el módulo de una sola vez sin verificación intermedia.