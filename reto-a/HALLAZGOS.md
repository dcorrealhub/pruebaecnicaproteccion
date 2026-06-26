# Code Review — `reto-a` · CIS Protección S.A.

**Fecha:** 2026-06-26
**Revisor:** Code Review Expert — Java / Spring Boot
**Rama:** `candidato/andres-giraldo`
**Stack:** Java 21 · Spring Boot 3.4.1 · H2 · JPA · Lombok

---

## Contexto de la Revisión

Observaciones identificadas durante la sesión de revisión, con el hallazgo que cada una originó.

---

**P1 — Revisión inicial del módulo**

> Revisar el módulo funcional existente dentro de la carpeta `reto-a`. Comenzar analizando
> `pom.xml`, luego verificar los tests. Utilizar SonarQube (token disponible) para enriquecer
> el análisis de calidad.

Hallazgos generados: 1.1–1.4 · 2.1–2.4 · 3.1–3.6 · 4.1–4.2 · 5 · 6.1–6.3 · 7.1–7.5

---

**P2 — Exposición del ID de afiliado en URL (`AporteController.java:39`)**

> En el endpoint `GET /api/aportes/consolidado?afiliadoId=AF-001&periodo=2025-06`,
> el identificador del afiliado se transmite como parámetro visible en la URL. En un sistema
> financiero, este dato es sensible. El principal riesgo es IDOR (Insecure Direct Object
> Reference): cualquier usuario autenticado puede sustituir `AF-001` por otro identificador
> y acceder a los aportes de un afiliado diferente, ya que el endpoint no valida que el
> recurso solicitado pertenezca al usuario que realiza la petición.

Hallazgo generado: **2.5 🔴**

---

**P3 — Credenciales de base de datos en texto plano (`application.properties:5-7`)**

> En `application.properties`, las propiedades `spring.datasource.url`, `username` y `password`
> están definidas en texto plano dentro del repositorio. No se utilizan variables de entorno
> para externalizar estos valores, lo que representa un riesgo de seguridad si el archivo
> llega a un ambiente productivo o queda expuesto en el historial de git.

Hallazgo generado: **6.4 🟠**

---

**P4 — ID incremental tipo `Long` en entidades de dominio (`Aporte.java:18`)**

> Las entidades de dominio, en particular `Aporte`, utilizan `GenerationType.IDENTITY` con
> tipo `Long`, generando identificadores secuenciales y predecibles (1, 2, 3…). Esto permite
> enumerar registros y revela el volumen de transacciones del sistema. Como mejora, se
> recomienda UUID. Si bien las transacciones están protegidas por JWT, la predictabilidad
> del ID sigue siendo un vector IDOR activo si algún endpoint no valida que el recurso
> pertenece al usuario autenticado.

Hallazgo generado: **4.3 🟠**

---

**P5 — Lombok sin configuración de annotation processor (`pom.xml`)**

> Lombok está declarado como dependencia en `pom.xml` pero no está configurado como
> annotation processor explícito en el `maven-compiler-plugin`, y no existe un archivo
> `lombok.config` en el módulo. Sin esta configuración, SonarQube analiza el bytecode
> generado por Lombok y lo reporta como código sin cobertura o con code smells, generando
> falsos positivos en el análisis de calidad.

Hallazgo generado: **1.5 🟠**

---

**P6 — Confirmación SonarQube: SQL dinámico y cobertura de tests**

> SonarQube reportó el siguiente hallazgo en `AporteController.java`:
>
> *"Make sure using a dynamically formatted SQL query is safe here"*
>
> Adicionalmente, el análisis de cobertura indica 0 % en los siguientes archivos:
> `AporteController.java`, `AporteService.java`, `EventoAporte.java`, `RetoAApplication.java`.

Hallazgos generados: **2.1 🔴** (confirmado) · **7.6 🟠** · **7.7 ⚪**

---

## Resumen Ejecutivo

| Severidad | Cantidad | Ejemplos clave |
|-----------|----------|----------------|
| 🔴 CRÍTICO | 5 | SQL Injection (confirmado Sonar), `afiliadoId` en URL (IDOR), `double` en dinero, bug `==`, sin `@Transactional` |
| 🟠 ALTO    | 9 | Race condition, `@Data` en JPA, H2 console abierta, sin `@Valid`, sin env vars en BD, Long ID predecible, Lombok sin configurar, sin cobertura en Controller/Service/EventoAporte, tope sin test |
| 🟡 MEDIO   | 6 | HTTP 200 en POST, `Saldo.mes` sin validar, sin `@Column`, PII en logs, solo `@SpringBootTest`, sin perfil prod |
| ⚪ BAJO    | 4 | `starter-jdbc` redundante, AF-003 sin test, sin `@DisplayName`, `RetoAApplication` sin test |

---

## 1. `pom.xml`

| # | Severidad | Hallazgo |
|---|-----------|----------|
| 1.1 | ⚪ BAJO | `spring-boot-starter-jdbc` es **redundante** — `spring-boot-starter-data-jpa` ya lo incluye transitivamente. Puede causar conflictos de autoconfiguración. |
| 1.2 | 🟠 ALTO | Falta `spring-boot-starter-validation` — sin él, las anotaciones `@Valid` / `@NotNull` del DTO no tienen efecto en ninguna capa. |
| 1.3 | 🟠 ALTO | **No hay plugin de SonarQube ni JaCoCo** configurados. Sin ellos no hay cobertura de código reportable ni análisis continuo de calidad. |
| 1.4 | 🟡 MEDIO | H2 en scope `runtime` (correcto para dev), pero **no hay perfil separado para producción**. Si se conecta una BD real, esta configuración no diferencia ambientes. |
| 1.5 | 🟠 ALTO | **Lombok sin configuración de annotation processor** — falta `annotationProcessorPaths` en el `maven-compiler-plugin` y no existe `lombok.config`. Ver detalle en sección 1.5. |

---

### 🟠 1.5 Lombok sin `annotationProcessorPaths` ni `lombok.config`

Lombok está declarado como dependencia pero **no como annotation processor explícito**. Esto
tiene dos consecuencias concretas:

**a) Compilación frágil** — en Maven + Java 21, la auto-detección vía ServiceLoader puede fallar
según la versión de `maven-compiler-plugin`. La configuración robusta es:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

**b) SonarQube reporta falsos positivos en código generado** — sin `lombok.config` que active
la anotación `@lombok.Generated`, Sonar analiza los métodos generados (`equals`, `hashCode`,
`toString`, constructores) y los cuenta como código sin cobertura o con smells.

Archivo necesario en la raíz del módulo `reto-a/lombok.config`:

```properties
# Marca el bytecode generado para que SonarQube lo excluya del análisis
lombok.addLombokGeneratedAnnotation = true
config.stopBubbling = true
```

---

## 2. `AporteController.java` — CRÍTICO

### 🔴 2.1 SQL Injection (`AporteController.java:41-43`) — confirmado por SonarQube

> **SonarQube:** *"Make sure using a dynamically formatted SQL query is safe here"*

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

### 🔴 2.5 IDOR — `afiliadoId` manipulable en URL (`AporteController.java:39`)

**Endpoint afectado:**

```
GET http://localhost:8080/api/aportes/consolidado?afiliadoId=AF-001&periodo=2025-06
```

**Clasificación:** OWASP A01:2021 — Broken Access Control (Insecure Direct Object Reference).

El endpoint recibe el `afiliadoId` directamente del cliente como query parameter y lo usa
para consultar datos **sin verificar que el recurso pertenece al usuario autenticado**. Un
atacante autenticado con un JWT válido solo necesita cambiar el valor del parámetro:

```
GET /api/aportes/consolidado?afiliadoId=AF-002&periodo=2025-06
GET /api/aportes/consolidado?afiliadoId=AF-003&periodo=2025-06
```

Cada petición devuelve los aportes de un afiliado diferente sin ninguna restricción. En un
sistema financiero esto constituye una fuga de información crítica entre clientes.

Riesgos secundarios asociados a la exposición en URL:

| Vector | Descripción |
|--------|-------------|
| **Logs de servidor / proxies** | Los query params quedan en texto plano en access logs de Nginx, ALB, API Gateway. El `afiliadoId` se registra en infraestructura fuera del control de la aplicación. |
| **Historial del navegador** | La URL completa se guarda en el historial y puede ser recuperada por otro usuario del mismo dispositivo. |
| **Referrer header** | Si el front-end enlaza recursos de terceros, el `afiliadoId` viaja en el header `Referer`. |

**Nota sobre JWT:** El JWT acredita *quién* hace la petición, pero no restringe *a qué afiliado*
puede consultar. La validación de autorización a nivel de recurso es responsabilidad del
servicio, no del token.

**Corrección recomendada:**

```java
// Resolver el afiliadoId desde el propio JWT — el cliente no puede manipularlo
@GetMapping("/consolidado")
public List<Aporte> consolidado(@RequestParam String periodo,
                                @AuthenticationPrincipal JwtUser user) {
    return service.consolidado(user.getAfiliadoId(), periodo);
}
```

Si un administrador necesita consultar por cualquier afiliado, ese caso debe ir por un
endpoint separado con rol explícito (`@PreAuthorize("hasRole('ADMIN')")`), nunca por el
mismo endpoint del afiliado.

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
| 7.6 | 🟠 ALTO | **Sin cobertura de tests en archivos clave** (detectado por SonarQube): `AporteController.java` (0 %), `AporteService.java` (parcial — solo paths felices via `@SpringBootTest`), `EventoAporte.java` (constructor de dominio no cubierto). Ver detalle en sección 7.6. |
| 7.7 | ⚪ BAJO | `RetoAApplication.java` sin test — el método `main` no está cubierto. Sonar lo reporta, aunque es aceptable excluirlo del umbral de cobertura. |

---

### 🟠 7.6 Detalle de cobertura faltante por archivo (SonarQube)

| Archivo | Cobertura | Paths no cubiertos |
|---------|-----------|-------------------|
| `AporteController.java` | **0 %** | Endpoint `registrar`, endpoint `consolidado`, `RowMapper` anónimo |
| `AporteService.java` | Parcial | Rama `nuevo > topeMensual` (bug del `==`), reset de `Saldo.mes`, evento fallido |
| `EventoAporte.java` | **0 %** | Constructor `EventoAporte(Aporte)` — la lógica de mapeo y `LocalDateTime.now()` no están cubiertos |
| `RetoAApplication.java` | **0 %** | Método `main` — aceptable excluirlo vía Sonar exclusion rules |

**Corrección prioritaria:**

```java
// Test de controller con MockMvc (cubre AporteController al 100 %)
@WebMvcTest(AporteController.class)
class AporteControllerTest {

    @Autowired MockMvc mvc;
    @MockBean AporteService service;
    @MockBean JdbcTemplate jdbc; // debe desaparecer con la corrección del 2.2

    @Test
    void consolidado_sqlInjection_retornaBadRequest() throws Exception {
        mvc.perform(get("/api/aportes/consolidado")
                .param("afiliadoId", "' OR '1'='1")
                .param("periodo", "2025-06"))
           .andExpect(status().isBadRequest()); // o 403, según la validación implementada
    }
}
```

---

## 8. Hallazgos por Archivo

```
reto-a/
├── pom.xml                          → 1.1 ⚪, 1.2 🟠, 1.3 🟠, 1.4 🟡, 1.5 🟠
├── lombok.config                    → FALTANTE (hallazgo 1.5)
├── src/main/
│   ├── java/.../RetoAApplication.java  → 7.7 ⚪ (sin test, main)
│   ├── java/.../controller/
│   │   └── AporteController.java    → 2.1 🔴, 2.2 🔴, 2.3 🟠, 2.4 🟡, 2.5 🔴 | cobertura 0 % (7.6)
│   ├── java/.../service/
│   │   └── AporteService.java       → 3.1 🔴, 3.2 🔴, 3.3 🔴, 3.4 🟠, 3.5 🟠, 3.6 🟡 | cobertura parcial (7.6)
│   ├── java/.../domain/
│   │   ├── Aporte.java              → 3.1 🔴, 4.1 🟠, 4.2 🟡, 4.3 🟠
│   │   ├── Saldo.java               → 3.1 🔴, 3.5 🟠, 4.1 🟠, 4.2 🟡, 4.3 🟠
│   │   └── EventoAporte.java        → 3.1 🔴, 4.1 🟠, 4.3 🟠 | cobertura 0 % (7.6)
│   ├── java/.../dto/
│   │   └── AporteRequest.java       → 3.1 🔴, 5 🟠
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

6. **Crear `lombok.config`** con `lombok.addLombokGeneratedAnnotation = true` y agregar `annotationProcessorPaths` en el `maven-compiler-plugin`.
7. **Agregar test `@WebMvcTest` para `AporteController`** — cubre SQL injection, status 201, validación de params.
8. **Agregar test unitario para `EventoAporte(Aporte)`** — verificar mapeo y `fechaEvento` no nula.
9. **Externalizar credenciales de BD** con variables de entorno (`${DB_URL}`, `${DB_USER}`, `${DB_PASSWORD}`).
10. **Migrar `Long id` a `UUID`** en `Aporte`, `Saldo`, `EventoAporte`.
11. Agregar `spring-boot-starter-validation` y anotar `AporteRequest` con `@NotBlank`, `@NotNull`, `@Positive`.
12. Agregar `@Valid` en `AporteController.registrar()`.
13. Agregar `@Lock(PESSIMISTIC_WRITE)` en `SaldoJpaRepository.findByAfiliadoId()`.
14. Desactivar `spring.h2.console.enabled` o protegerlo con Spring Security.
15. Reemplazar `@Data` por `@Getter`/`@Setter` en entidades JPA.
16. Validar `Saldo.mes` contra el periodo actual en el servicio.

### Fase 3 — Mediano plazo (cobertura y observabilidad)

17. Agregar unit tests con Mockito para `AporteService` (sin `@SpringBootTest`).
18. Agregar `@Transactional` en los tests de integración.
19. Crear test para tope mensual y para el endpoint `consolidado`.
20. Configurar JaCoCo + plugin de SonarQube en `pom.xml`.
21. Separar `application-dev.properties` y `application-prod.properties`.
22. Retornar `ResponseEntity` con `201 Created` en el POST.
23. Excluir `RetoAApplication.java` del umbral de cobertura en Sonar (`sonar.coverage.exclusions`).

---

*Revisión estática manual completada — pendiente correlacionar con análisis SonarQube.*
