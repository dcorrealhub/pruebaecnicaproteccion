# Code Review — `reto-a` · CIS Protección S.A.

**Fecha:** 2026-06-26
**Revisor:** Code Review Expert — Java / Spring Boot
**Rama:** `candidato/andres-giraldo`
**Stack:** Java 21 · Spring Boot 3.4.1 · H2 · JPA · Lombok

---

## Contexto de la Revisión

La siguiente tabla registra los prompts y observaciones realizadas durante la sesión de revisión,
junto con los hallazgos que cada uno originó.

| # | Prompt / Observación del revisor | Hallazgos generados |
|---|----------------------------------|---------------------|
| P1 | "Revisar módulo funcional en `reto-a`: `pom.xml` primero, luego tests, y análisis con SonarQube." | 1.1–1.4, 2.1–2.4, 3.1–3.6, 4.1–4.2, 5, 6.1–6.3, 7.1–7.5 |
| P2 | "En un método GET se mandan las variables de consulta por la URL — el ID del afiliado va expuesto. Si el identificador es información sensible, esto aumenta el riesgo de divulgación de información. Además, la API debe validar la autorización del usuario para evitar vulnerabilidades como IDOR (Insecure Direct Object Reference). `GET /api/aportes/consolidado?afiliadoId=AF-001&periodo=2025-06`" | 2.5 🔴 |
| P3 | "En `application.properties` no se usan variables de entorno, sobre todo en las variables de la BD." | 6.4 🟠 |
| P4 | "En los dominios, al menos en `Aporte`, se usa ID incremental tipo `Long` — hace los registros predecibles. Como mejora usar UUID. Se puede justificar que se usa JWT para comprobación de transacciones, pero sigue siendo un punto vulnerable." | 4.3 🟠 |

---

## Resumen Ejecutivo

| Severidad | Cantidad | Ejemplos clave |
|-----------|----------|----------------|
| 🔴 CRÍTICO | 5 | SQL Injection, `afiliadoId` expuesto en URL (XSS), `double` en dinero, bug `==`, sin `@Transactional` |
| 🟠 ALTO    | 7 | Race condition, `@Data` en JPA, H2 console abierta, sin `@Valid`, sin env vars en BD, Long ID predecible, tope sin test |
| 🟡 MEDIO   | 6 | HTTP 200 en POST, `Saldo.mes` sin validar, sin `@Column`, PII en logs, solo `@SpringBootTest`, sin perfil prod |
| ⚪ BAJO    | 3 | `starter-jdbc` redundante, AF-003 sin test, sin `@DisplayName` |

---

## 1. `pom.xml`

| # | Severidad | Hallazgo |
|---|-----------|----------|
| 1.1 | ⚪ BAJO | `spring-boot-starter-jdbc` es **redundante** — `spring-boot-starter-data-jpa` ya lo incluye transitivamente. Puede causar conflictos de autoconfiguración. |
| 1.2 | 🟠 ALTO | Falta `spring-boot-starter-validation` — sin él, las anotaciones `@Valid` / `@NotNull` del DTO no tienen efecto en ninguna capa. |
| 1.3 | 🟠 ALTO | **No hay plugin de SonarQube ni JaCoCo** configurados. Sin ellos no hay cobertura de código reportable ni análisis continuo de calidad. |
| 1.4 | 🟡 MEDIO | H2 en scope `runtime` (correcto para dev), pero **no hay perfil separado para producción**. Si se conecta una BD real, esta configuración no diferencia ambientes. |

---

## 2. `AporteController.java` — CRÍTICO

### 🔴 2.1 SQL Injection (`AporteController.java:41-43`)

```java
// VULNERABLE — concatenación directa de parámetros del usuario
String sql = "SELECT * FROM aporte WHERE afiliado_id = '"
        + afiliadoId + "' AND periodo = '" + periodo + "'";
return jdbc.query(sql, aporteRowMapper);
```

Un atacante puede enviar `' OR '1'='1` como `afiliadoId` y obtener **todos** los registros de la tabla.
Clasificación: **OWASP A03:2021 — Injection**.

**Corrección mínima** — usar parámetros posicionales:

```java
jdbc.query(
    "SELECT * FROM aporte WHERE afiliado_id = ? AND periodo = ?",
    aporteRowMapper, afiliadoId, periodo
);
```

**Corrección correcta** — eliminar `JdbcTemplate` del controlador (ver punto 2.2).

---

### 🔴 2.2 `JdbcTemplate` en el Controller (violación de arquitectura)

El controlador inyecta `JdbcTemplate` directamente y ejecuta SQL, violando:

- **SRP** — el controller mezcla presentación con acceso a datos.
- **Arquitectura en capas** — Controller → Service → Repository.

Además, `AporteJpaRepository` **ya tiene** `findByAfiliadoIdAndPeriodo()` que hace exactamente lo mismo,
convirtiendo al endpoint `consolidado` en código duplicado e innecesariamente peligroso.

---

### 🟠 2.3 Sin `@Valid` en el request body

```java
// Actual (incorrecto)
public Aporte registrar(@RequestBody AporteRequest req)

// Correcto
public Aporte registrar(@RequestBody @Valid AporteRequest req)
```

Sin `@Valid`, ninguna anotación de validación del DTO es evaluada por Spring.

---

### 🟡 2.4 HTTP Status incorrecto en POST

El endpoint `@PostMapping` devuelve `200 OK` en lugar de `201 Created`.
Debe retornar `ResponseEntity<Aporte>` con `HttpStatus.CREATED`.

---

### 🔴 2.5 `afiliadoId` expuesto en URL — riesgo XSS / exposición de PII

**Endpoint afectado:**

```
GET http://localhost:8080/api/aportes/consolidado?afiliadoId=AF-001&periodo=2025-06
```

Enviar un identificador sensible de afiliado como query parameter de un `GET` tiene múltiples
vectores de riesgo:

| Vector | Descripción |
|--------|-------------|
| **XSS reflejado** | Un atacante construye una URL maliciosa con código en el parámetro `afiliadoId`. Si la respuesta se renderiza en un front sin sanitizar, el script se ejecuta en el navegador de la víctima. |
| **Logs de servidor / proxies** | Los query params quedan en texto plano en access logs de Nginx, ALB, API Gateway y cualquier proxy intermedio. El `afiliadoId` queda registrado en infraestructura fuera del control de la app. |
| **Historial del navegador** | La URL completa —con el ID de afiliado— se guarda en el historial y puede ser recuperada por otro usuario del mismo dispositivo. |
| **Referrer header** | Si la página enlaza a terceros, el `afiliadoId` viaja en el header `Referer` de las peticiones subsecuentes. |

**Clasificación:** OWASP A01:2021 — Broken Access Control / A07:2021 — Identification and
Authentication Failures.

**Nota sobre JWT:** Aunque el sistema usa JWT para validar las transacciones, el token protege
*quién* puede hacer la petición, pero **no protege la exposición del ID en la URL**. El
`afiliadoId` sigue viajando en claro en logs, proxies e historial antes de que el JWT sea
verificado.

**Corrección recomendada:**

```java
// Opción 1 — tomar el afiliadoId desde el JWT, no del query param
@GetMapping("/consolidado")
public List<Aporte> consolidado(@RequestParam String periodo,
                                @AuthenticationPrincipal JwtUser user) {
    return service.consolidado(user.getAfiliadoId(), periodo);
}

// Opción 2 — si se mantiene como param, moverlo a header o al body de un POST
// (POST semántico de búsqueda, práctica aceptada para payloads sensibles)
```

---

## 3. `AporteService.java` — CRÍTICO

### 🔴 3.1 `double` para valores monetarios

```java
// En AporteService
@Value("${aporte.tope-mensual:10000000}")
private double topeMensual;

@Value("${aporte.umbral-revision:5000000}")
private double umbralRevision;

// En las entidades Aporte y Saldo
private double monto;
private double totalMes;
```

`double` / `float` no son adecuados para dinero. La representación IEEE 754 produce errores de
redondeo que se acumulan silenciosamente en operaciones financieras. El estándar de la industria
es **`BigDecimal`**.

---

### 🔴 3.2 Comparación `==` con `double` — bug de lógica (`AporteService.java:45`)

```java
if (nuevo == topeMensual) {   // BUG: nunca se cumple con doubles
    throw new IllegalArgumentException("El monto supera el tope mensual...");
}
```

Dos problemas combinados:

1. `==` con `double` es impreciso — `9_999_999.9 + 0.1 == 10_000_000.0` puede evaluar `false`.
2. La condición solo bloquea en **igualdad exacta**, no cuando `nuevo > topeMensual`.
   Un aporte de $10,000,001 pasaría sin bloquearse.

**Condición correcta:**

```java
if (nuevo > topeMensual) {
    throw new IllegalArgumentException("El monto supera el tope mensual permitido");
}
```

---

### 🔴 3.3 Sin `@Transactional` — riesgo de inconsistencia de datos

```java
public Aporte registrar(AporteRequest req) {
    saldoRepo.save(s);              // ← se persiste
    eventoRepo.save(...);           // ← si falla aquí...
    return aporteRepo.save(aporte); // ← el aporte nunca se registra
}
```

Sin `@Transactional`, si falla alguna operación intermedia, el saldo queda actualizado
pero el aporte no existe. **Violación de atomicidad (ACID).**

**Corrección:**

```java
@Transactional
public Aporte registrar(AporteRequest req) { ... }
```

---

### 🟠 3.4 Race condition en la validación del tope mensual

El patrón read-check-write no es atómico bajo concurrencia:

```
Hilo A: lee saldo = 8M → calcula 8M + 3M = 11M → ¿supera? No (bug ==) → guarda 11M
Hilo B: lee saldo = 8M → calcula 8M + 3M = 11M → ¿supera? No              → guarda 11M
```

Ambos hilos superan el tope. Solución: `@Lock(LockModeType.PESSIMISTIC_WRITE)` en
`SaldoJpaRepository.findByAfiliadoId`, o campo `@Version` para bloqueo optimista.

---

### 🟠 3.5 `Saldo.mes` nunca se valida contra el mes actual

Si un afiliado tiene un registro de saldo del mes anterior, el acumulado se suma al mes viejo
sin resetear. El servicio no verifica que `saldo.getMes().equals(periodoActual)`.

---

### 🟡 3.6 Log de datos de afiliado (PII)

```java
log.info("Aporte registrado: monto={} afiliado={}", monto, req.getAfiliadoId());
```

El `afiliadoId` puede ser un dato personal sensible. En entornos regulados (datos financieros,
normativa de protección de datos) debe evaluarse si es correcto loguearlo en texto plano.

---

## 4. Entidades de Dominio

### 🟠 4.1 `@Data` en entidades JPA — anti-patrón conocido

`@Data` genera `equals()` / `hashCode()` basado en **todos** los campos, incluyendo el `id`
mutable y posibles asociaciones lazy. Esto rompe colecciones (`Set`, `HashMap`) durante el
ciclo de vida de JPA, y el `toString()` generado puede disparar carga lazy no deseada.

**Alternativa correcta:**

```java
@Getter
@Setter
@ToString(exclude = {"campo_lazy"})
// equals/hashCode implementados manualmente por clave de negocio o solo por id con null-check
```

---

### 🟡 4.2 Sin restricciones `@Column` en las entidades

Ninguna columna declara `nullable = false`, `length` o `unique`. Las restricciones de integridad
solo existen a nivel Java; el esquema de BD no las refleja, lo que permite inconsistencias
directas vía SQL o migraciones.

---

### 🟠 4.3 ID incremental tipo `Long` — registros predecibles (`Aporte.java:18`)

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

Un ID de tipo `Long` con estrategia `IDENTITY` genera valores secuenciales (1, 2, 3…).
Esto hace que los registros sean **enumerables y predecibles**:

- Un atacante puede iterar IDs para descubrir registros a los que no debería acceder.
- Revela el volumen de transacciones del sistema (información de negocio sensible).
- Facilita ataques de tipo IDOR (Insecure Direct Object Reference).

**Nota sobre JWT:** El JWT protege *el acceso autenticado*, pero si un endpoint acepta un ID
como parámetro sin verificar que el recurso pertenece al usuario autenticado, la predictabilidad
del ID amplifica el vector de ataque. La protección JWT no elimina este riesgo; lo mitiga
parcialmente.

**Corrección recomendada — UUID v4:**

```java
@Id
@GeneratedValue(strategy = GenerationType.UUID)
@Column(updatable = false, nullable = false)
private UUID id;
```

O en versiones anteriores a JPA 3.1:

```java
@Id
@GeneratedValue(generator = "UUID")
@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
private String id;
```

Aplica a las tres entidades: `Aporte`, `Saldo`, `EventoAporte`.

---

## 5. `AporteRequest.java` — DTO sin validación

```java
private String afiliadoId;   // falta @NotBlank
private double monto;        // falta @Positive — además debería ser BigDecimal
private String canal;        // falta @NotBlank, sin validación de valores permitidos
```

---

## 6. `application.properties`

| # | Severidad | Hallazgo |
|---|-----------|----------|
| 6.1 | 🟠 ALTO | `spring.h2.console.enabled=true` — la consola H2 está expuesta en `/h2-console` **sin autenticación**. Cualquiera con acceso de red puede inspeccionar o modificar la BD. |
| 6.2 | 🟠 ALTO | `spring.datasource.password=` — contraseña vacía. Aceptable en H2 local, pero peligroso si esta configuración se propaga a producción por error. |
| 6.3 | 🟡 MEDIO | `ddl-auto=create-drop` destruye el esquema al parar la aplicación. No hay separación de perfiles (`application-dev.properties` / `application-prod.properties`). |
| 6.4 | 🟠 ALTO | **Las credenciales de BD están en texto plano**, no en variables de entorno. `spring.datasource.url`, `username` y `password` deben externalizarse con `${VAR}` para que cada ambiente inyecte sus propios valores sin que las credenciales lleguen al repositorio. |

**Corrección para 6.4:**

```properties
# application.properties — valores inyectados desde el entorno
spring.datasource.url=${DB_URL:jdbc:h2:mem:retodb}
spring.datasource.username=${DB_USER:sa}
spring.datasource.password=${DB_PASSWORD:}
```

Las variables reales se configuran en el servidor (variables de entorno del SO, Secrets Manager,
Vault, o el sistema de configuración del cluster) y **nunca se commitean** en el repositorio.

---

## 7. Tests (`AporteServiceTest.java`)

| # | Severidad | Hallazgo |
|---|-----------|----------|
| 7.1 | 🟡 MEDIO | Solo tests de integración con `@SpringBootTest` — no hay unit tests con mocks. El contexto completo carga para cada suite, haciendo los tests lentos y frágiles. |
| 7.2 | 🟠 ALTO | **Sin `@Transactional` en los tests** — el estado de BD persiste entre tests. `registrar_montoSuperaUmbral_marcaRevision` asume que AF-002 empieza en saldo 0; si otro test lo modifica antes, falla. |
| 7.3 | 🟠 ALTO | **No hay test para el tope mensual** — el bug del `==` en `AporteService:45` no está cubierto por ningún caso. |
| 7.4 | 🟡 MEDIO | **No hay test para el endpoint `consolidado`** — la vulnerabilidad de SQL injection no tiene cobertura de ningún tipo. |
| 7.5 | ⚪ BAJO | AF-003 tiene saldo inicial de 4.5M en `data.sql` pero no hay ningún test que lo use. Dato muerto. |

---

## 8. Hallazgos por Archivo

```
reto-a/
├── pom.xml                          → 1.1, 1.2, 1.3, 1.4
├── src/main/
│   ├── java/.../controller/
│   │   └── AporteController.java    → 2.1 🔴, 2.2 🔴, 2.3 🟠, 2.4 🟡, 2.5 🔴
│   ├── java/.../service/
│   │   └── AporteService.java       → 3.1 🔴, 3.2 🔴, 3.3 🔴, 3.4 🟠, 3.5 🟠, 3.6 🟡
│   ├── java/.../domain/
│   │   ├── Aporte.java              → 3.1 🔴 (double), 4.1 🟠, 4.2 🟡, 4.3 🟠
│   │   ├── Saldo.java               → 3.1 🔴 (double), 3.5 🟠, 4.1 🟠, 4.2 🟡, 4.3 🟠
│   │   └── EventoAporte.java        → 3.1 🔴 (double), 4.1 🟠, 4.3 🟠
│   ├── java/.../dto/
│   │   └── AporteRequest.java       → 3.1 🔴 (double), 5 🟠
│   └── resources/
│       └── application.properties   → 6.1 🟠, 6.2 🟠, 6.3 🟡, 6.4 🟠
└── src/test/
    └── java/.../AporteServiceTest.java → 7.1 🟡, 7.2 🟠, 7.3 🟠, 7.4 🟡, 7.5 ⚪
```

---

## 9. Prioridad de Corrección

### Fase 1 — Inmediata (bloquea producción)

1. **Eliminar SQL Injection** — parametrizar `jdbc.query` o eliminar `JdbcTemplate` del controller y usar `AporteJpaRepository.findByAfiliadoIdAndPeriodo()`.
2. **Sacar `afiliadoId` de la URL** — resolverlo desde el JWT (`@AuthenticationPrincipal`) o moverlo a header/body.
3. **Agregar `@Transactional`** en `AporteService.registrar()`.
4. **Corregir comparación del tope** — cambiar `nuevo == topeMensual` por `nuevo > topeMensual`.
5. **Migrar `double` a `BigDecimal`** en entidades y servicio.

### Fase 2 — Corto plazo (calidad y seguridad)

6. **Externalizar credenciales de BD** con variables de entorno (`${DB_URL}`, `${DB_USER}`, `${DB_PASSWORD}`).
7. **Migrar `Long id` a `UUID`** en `Aporte`, `Saldo`, `EventoAporte`.
8. Agregar `spring-boot-starter-validation` y anotar `AporteRequest` con `@NotBlank`, `@NotNull`, `@Positive`.
9. Agregar `@Valid` en `AporteController.registrar()`.
10. Agregar `@Lock(PESSIMISTIC_WRITE)` en `SaldoJpaRepository.findByAfiliadoId()`.
11. Desactivar `spring.h2.console.enabled` o protegerlo con Spring Security.
12. Reemplazar `@Data` por `@Getter`/`@Setter` en entidades JPA.
13. Validar `Saldo.mes` contra el periodo actual en el servicio.

### Fase 3 — Mediano plazo (cobertura y observabilidad)

14. Agregar unit tests con Mockito para `AporteService` (sin `@SpringBootTest`).
15. Agregar `@Transactional` en los tests de integración.
16. Crear test para tope mensual y para el endpoint `consolidado`.
17. Configurar JaCoCo + plugin de SonarQube en `pom.xml`.
18. Separar `application-dev.properties` y `application-prod.properties`.
19. Retornar `ResponseEntity` con `201 Created` en el POST.

---

*Revisión estática manual completada — pendiente correlacionar con análisis SonarQube.*
