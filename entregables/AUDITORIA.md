# Auditoría de Código — Reto A
**Módulo:** Servicio de registro de aportes voluntarios  
**Revisado por:** Juan Esteban Valdes  
**Autor del MR:** Lucho Díaz  
**Fecha:** 2026-06-26


---
CONTEXTO:
Lucho Díaz es el desarrollador junior del equipo. Esta es la revisión de su MR para el módulo de registro de aportes voluntarios.

---




Lucho, gracias por el MR. El módulo compila y los tests pasan, lo que es un buen punto de partida. Dicho esto, encontré varios puntos que hay que resolver antes de aprobar — algunos son bloqueantes dado el contexto financiero regulado en el que corre este servicio.


## Hallazgos de Arquitectura

### H1 — Modelo de dominio acoplado a JPA
**Severidad:** Alta 🟠  
**Archivos:** `domain/Aporte.java`, `domain/Saldo.java`, `domain/EventoAporte.java`, `repository/AporteJpaRepository.java`

Las clases de dominio tienen anotaciones JPA (`@Entity`, `@Table`, `@Id`, `@GeneratedValue`) directamente sobre ellas. El repositorio trabaja con estos mismos objetos, lo que acopla el dominio a la infraestructura de persistencia y viola la regla de dependencia de Clean Architecture: la infraestructura debe depender del dominio, nunca al revés.

Consecuencias:
1. Si se cambia de JPA a otro mecanismo de persistencia, hay que tocar el dominio.
2. Cualquier cambio en el esquema de BD impacta directamente el modelo de negocio.
3. El dominio no es un POJO puro — no se puede probar sin levantar el contexto de JPA.

**Corrección:**  
Separar en dos objetos distintos:

- **`domain/Aporte.java`** — POJO puro, sin ninguna anotación de infraestructura.
- **`infrastructure/persistence/AporteEntity.java`** — tiene las anotaciones JPA, mapea a la tabla en BD.

El adaptador de persistencia se encarga de convertir entre `AporteEntity` ↔ `Aporte`. El dominio queda completamente aislado de la tecnología de persistencia.

---

### H2 — Violación de capas + SQL Injection en el controller
**Severidad:** Crítica 🔴  
**Archivo:** `controller/AporteController.java`  
**Método:** `consolidado()`

El controller inyecta `JdbcTemplate` y construye la query concatenando los parámetros del request directamente:

```java
String sql = "SELECT * FROM aporte WHERE afiliado_id = '"
        + afiliadoId + "' AND periodo = '" + periodo + "'";
return jdbc.query(sql, aporteRowMapper);
```

Dos problemas en uno:

1. **Violación de capas**: la responsabilidad de persistencia pertenece al repositorio, no al controller. Ya existe `AporteJpaRepository.findByAfiliadoIdAndPeriodo` que resuelve exactamente esto.
2. **SQL Injection (OWASP A03)**: un atacante puede enviar `afiliadoId = AF-001' OR '1'='1` y obtener los aportes de todos los afiliados. En un entorno financiero regulado esto es inaceptable.

En un proyecto que ya usa Spring Data JPA, el `JdbcTemplate` raw en el controller no tiene razón de existir.

**Corrección:**

```java
@GetMapping("/consolidado")
public List<Aporte> consolidado(@RequestParam String afiliadoId,
                                @RequestParam String periodo) {
    return service.consolidado(afiliadoId, periodo);
}
```

```java
// AporteService
public List<Aporte> consolidado(String afiliadoId, String periodo) {
    return aporteRepo.findByAfiliadoIdAndPeriodo(afiliadoId, periodo);
}
```

---

### H3 — Método `registrar` con múltiples responsabilidades y sin `@Transactional`
**Severidad:** Crítica 🔴  
**Archivo:** `service/AporteService.java`

El método `registrar` mezcla validación, lógica de negocio, persistencia y emisión de eventos en un solo bloque. Esto viola el principio de responsabilidad única (SOLID) y hace el código difícil de mantener y probar.

El problema más grave es que realiza tres escrituras a la BD sin `@Transactional`:
1. `saldoRepo.save(s)` — actualiza el saldo acumulado
2. `eventoRepo.save(...)` — guarda el evento de auditoría
3. `aporteRepo.save(aporte)` — guarda el aporte

Si el sistema falla entre cualquiera de estas operaciones, el estado queda inconsistente. En un sistema financiero esto es inaceptable. Adicionalmente, el evento se guarda **antes** que el aporte — si falla `aporteRepo.save`, el evento queda en BD referenciando un aporte que no existe.

**Corrección:**  
1. Anotar el método con `@Transactional` — o todo pasa o nada pasa.
2. Separar responsabilidades:
   - Validación → método privado `validarAporte()` o clase `AporteValidator`
   - Lógica de negocio → encapsular en el dominio
   - Eventos → emitirlos **después** de confirmar la persistencia del aporte

```java
@Transactional
public Aporte registrar(AporteRequest req) {
    validarAporte(req);
    // lógica de negocio...
    Aporte aporte = aporteRepo.save(aporte);
    eventoRepo.save(new EventoAporte(aporte)); // siempre después del aporte
    return aporte;
}
```

---

### H4 — Pruebas mal clasificadas e insuficiente cobertura
**Severidad:** Alta 🟠  
**Archivo:** `test/AporteServiceTest.java`

Los tests usan `@SpringBootTest`, lo que levanta todo el contexto de Spring y la BD. Son tests de integración disfrazados de unitarios — lentos, con dependencia de datos compartidos (`data.sql`) y sin aislamiento entre pruebas. Si un test modifica el saldo de `AF-001`, el siguiente test que use ese afiliado puede fallar.

**Corrección:**  
Separar claramente en dos tipos:

- **`AporteServiceTest.java`** — test unitario con `@ExtendWith(MockitoExtension.class)`, mockea los repositorios, prueba la lógica del service en aislamiento. Rápido.
- **`AporteServiceIntegrationTest.java`** — test de integración con `@SpringBootTest`, BD real, valida que todo el flujo encaja.

**Cobertura:**  
El criterio de calidad del equipo define el umbral mínimo (referencia estándar: 80%). Los tests deben cubrir:
- Todos los caminos felices
- Todas las excepciones definidas (monto negativo, monto cero, afiliado inexistente, tope mensual superado)
- Idempotencia — doble registro del mismo aporte

---

## Hallazgos Puntuales

### H5 — Tipo `double` para valores monetarios en todo el módulo
**Severidad:** Crítica 🔴  
**Archivos:** `dto/AporteRequest.java`, `domain/Aporte.java`, `domain/Saldo.java`, `domain/EventoAporte.java`, `service/AporteService.java`

**Fundamento de negocio:**  
El Banco de la República de Colombia eliminó los centavos en 1993. La unidad mínima de la moneda colombiana es el peso entero — esto define el tipo de dato correcto para todo el sistema.

`double` usa representación binaria de punto flotante (IEEE 754), lo que produce errores de precisión acumulables:

```java
System.out.println(0.1 + 0.2); // 0.30000000000000004
```

En un sistema regulado por la SFC, una diferencia de un peso por error de representación es un bug de compliance.

**Corrección:**  
Dado que este módulo solo realiza sumas y comparaciones, usar `long` en todo el módulo:

```java
private long monto;        // AporteRequest, Aporte, EventoAporte
private long totalMes;     // Saldo
private long topeMensual;  // AporteService
private long umbralRevision;
```

> **Nota de evolución:** si el sistema incorpora cálculos con decimales intermedios (tasas de interés, rendimientos, proyecciones), migrar a `BigDecimal`, que ofrece precisión exacta y control explícito del modo de redondeo.

---

### H6 — Condición del tope mensual incorrecta
**Severidad:** Crítica 🔴  
**Archivo:** `service/AporteService.java`

```java
if (nuevo == topeMensual) {
    throw new IllegalArgumentException("El monto supera el tope mensual permitido");
}
```

Dos errores en uno:
1. Con `double`, la igualdad exacta con `==` prácticamente nunca se cumple por los errores de precisión de punto flotante.
2. Aunque se corrija el tipo a `long`, la condición sigue siendo incorrecta — si el tope es `10.000.000` y el nuevo total es `10.000.001`, la condición es `false` y el aporte pasa superando el tope.

**Corrección:**

```java
if (nuevo >= topeMensual) {
    throw new IllegalArgumentException("El monto supera el tope mensual permitido");
}
```

---

### H7 — `findByAfiliadoId` no filtra por mes
**Severidad:** Crítica 🔴  
**Archivo:** `repository/SaldoJpaRepository.java`, `service/AporteService.java`

El saldo se busca solo por `afiliadoId`, sin considerar el mes. Esto hace que el sistema acumule el saldo de meses anteriores sobre el mes actual. Ejemplo: si un afiliado acumuló `$8.000.000` en enero y en febrero quiere aportar `$9.000.000`, el sistema suma `17.000.000` y rechaza el aporte — cuando en febrero debería partir de `$0`.

**Corrección:**

```java
saldoRepo.findByAfiliadoIdAndMes(afiliadoId, LocalDate.now().withDayOfMonth(1))
```

---

### H8 — Sin idempotencia
**Severidad:** Crítica 🔴  
**Archivo:** `service/AporteService.java`

No existe ningún mecanismo para detectar un aporte duplicado. Si el cliente envía la misma petición dos veces (reintento de red, doble clic), el sistema registra el aporte dos veces — al afiliado le descuentan el doble.

**Corrección:**  
Manejar un `idempotencyKey` (UUID generado por el cliente) que se persiste con el aporte:

```java
if (aporteRepo.existsByIdempotencyKey(req.getIdempotencyKey())) {
    return aporteRepo.findByIdempotencyKey(req.getIdempotencyKey());
}
```

---

### H9 — Campo derivado `periodo` persistido innecesariamente
**Severidad:** Media 🟡  
**Archivos:** `domain/Aporte.java`, `service/AporteService.java`, `repository/AporteJpaRepository.java`

`periodo` es un valor que siempre se puede calcular a partir de `fecha`. Persistirlo como columna independiente abre la puerta a inconsistencias — nada impide que `fecha = 2025-06-15` y `periodo = "2025-05"` coexistan en la misma fila.

**Corrección:**  
Eliminar `periodo` como columna y filtrar por rango de fechas:

```java
List<Aporte> findByAfiliadoIdAndFechaBetween(String afiliadoId, 
                                              LocalDate inicio, 
                                              LocalDate fin);
```

---

### H10 — Fecha de `Saldo` guardada como `String`
**Severidad:** Media 🟡  
**Archivo:** `domain/Saldo.java`

El campo `mes` está guardado como `String` con formato `"yyyy-MM"`. Sin validación de formato, sin posibilidad de comparaciones temporales confiables, e inconsistente con `Aporte.fecha` que ya usa `LocalDate`.

**Corrección:**

```java
private LocalDate mes; // ej: 2025-06-01
```

---

### H11 — Sin manejador global de excepciones
**Severidad:** Alta 🟠  
**Archivo:** No existe ningún `@ControllerAdvice` en el módulo

Las excepciones de negocio (`IllegalArgumentException`) no están mapeadas a respuestas HTTP apropiadas. Spring las convierte en 500 cuando deberían ser 400.

**Corrección:**

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleValidacion(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
```

---

### H12 — Exposición de datos sensibles en logs y BD
**Severidad:** Alta 🟠  
**Archivo:** `service/AporteService.java`

```java
log.info("Aporte registrado: monto={} afiliado={}", monto, req.getAfiliadoId());
```

Los logs de aplicación son para trazabilidad técnica, no para datos financieros de afiliados. Para eso existe la auditoría de negocio — que es justamente el propósito de `EventoAporte`. El registro de quién hizo qué y por cuánto debe vivir en una tabla de auditoría controlada, con acceso restringido y bajo cumplimiento normativo.

**Nota de revisión de seguridad:**  
Dependiendo de la clasificación del dato definida por el equipo de seguridad y compliance, `afiliadoId` y `monto` en la tabla de aportes pueden requerir **encriptación en reposo en la BD**. Esta decisión debe tomarse antes de ir a producción y está sujeta a los requerimientos de la SFC.

---

### H13 — Configuración insegura para producción
**Severidad:** Crítica 🔴  
**Archivo:** `resources/application.properties`

**Problema bloqueante:**  
`spring.jpa.hibernate.ddl-auto=create-drop` elimina y recrea todas las tablas cada vez que arranca la aplicación. En producción esto significa **pérdida total de datos**. Debe ser `validate`.

**Notas a revisar antes de producción:**
- Las credenciales de BD están hardcodeadas en texto plano. Deben externalizarse a variables de entorno: `${DB_USERNAME}`, `${DB_PASSWORD}`, `${DB_URL}`.
- `spring.h2.console.enabled=true` expone una interfaz web con acceso directo a la BD. Debe deshabilitarse fuera de desarrollo.

---

### H14 — Nombres de variables no descriptivos
**Severidad:** Baja 🟢  
**Archivo:** `service/AporteService.java`

Variables como `s` y `nuevo` no comunican su intención:

```java
Saldo s = saldoRepo.findByAfiliadoId(req.getAfiliadoId());
double nuevo = s.getTotalMes() + monto;
```

**Corrección:**

```java
Saldo saldoMensual = saldoRepo.findByAfiliadoId(req.getAfiliadoId());
long totalAcumulado = saldoMensual.getTotalMes() + monto;
```

---

## Resumen

| # | Severidad | Hallazgo |
|---|-----------|----------|
| H1 | 🟠 Alta | Modelo de dominio acoplado a JPA |
| H2 | 🔴 Crítica | Violación de capas + SQL Injection en controller |
| H3 | 🔴 Crítica | Método `registrar` sin `@Transactional` y con múltiples responsabilidades |
| H4 | 🟠 Alta | Pruebas mal clasificadas e insuficiente cobertura |
| H5 | 🔴 Crítica | `double` para valores monetarios en todo el módulo |
| H6 | 🔴 Crítica | Condición del tope mensual incorrecta (`==` en vez de `>=`) |
| H7 | 🔴 Crítica | `findByAfiliadoId` no filtra por mes |
| H8 | 🔴 Crítica | Sin idempotencia |
| H9 | 🟡 Media | Campo derivado `periodo` persistido innecesariamente |
| H10 | 🟡 Media | Fecha de `Saldo` guardada como `String` |
| H11 | 🟠 Alta | Sin manejador global de excepciones |
| H12 | 🟠 Alta | Exposición de datos sensibles en logs y BD |
| H13 | 🔴 Crítica | Configuración insegura para producción |
| H14 | 🟢 Baja | Nombres de variables no descriptivos |

El esfuerzo se nota y la estructura general del módulo tiene sentido. Con estas correcciones el código va a estar en un nivel adecuado para un entorno regulado.

Si algún hallazgo no quedó claro o quieres discutir el enfoque antes de hacer los cambios, agéndame una reunión y lo revisamos juntos — mejor aclarar antes que corregir dos veces.
