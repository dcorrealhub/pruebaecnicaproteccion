# Hallazgos de revisión — Merge Request `reto-a`

Revisión técnica del módulo de aportes voluntarios. A continuación se detallan los
hallazgos encontrados, ordenados según se reportaron en la revisión, con su nivel de
riesgo, el diagnóstico del problema y la corrección propuesta.

---

## 1. Inyección SQL en el endpoint de consulta

**Riesgo:** Crítico

**Descripción**
En el endpoint para consultar los aportes, los parámetros de la URL (`afiliadoId` y `periodo`) se están pegando directamente con texto (comillas y signos de `+`) para armar la consulta a la base de datos. Si un usuario malintencionado escribe comandos de SQL en la URL en vez de un ID real, puede saltarse los filtros, ver la plata de cualquier otro cliente o hasta borrar tablas.

**Solución**
Para la corrección se debe eliminar el `String sql` del controlador y reutilizar el repositorio `repository/AporteJpaRepository.java` que ya existe y el cual contiene una función específica para este controlador que no está siendo usada: `AporteJpaRepository.findByAfiliadoIdAndPeriodo`.

**Ejemplo de corrección**

Antes (`controller/AporteController.java`):

```java
@GetMapping("/consolidado")
public List<Aporte> consolidado(@RequestParam String afiliadoId,
                                @RequestParam String periodo) {
    String sql = "SELECT * FROM aporte WHERE afiliado_id = '"
            + afiliadoId + "' AND periodo = '" + periodo + "'";
    return jdbc.query(sql, aporteRowMapper);
}
```

Después:

```java
@GetMapping("/consolidado")
public List<Aporte> consolidado(@RequestParam String afiliadoId,
                                @RequestParam String periodo) {
    return aporteRepo.findByAfiliadoIdAndPeriodo(afiliadoId, periodo);
}
```

Se elimina la dependencia de `JdbcTemplate` y del `RowMapper` manual en el controlador; la consulta queda parametrizada por Spring Data JPA, que escapa los valores automáticamente.

---

## 2. Ausencia total de autenticación y autorización

**Riesgo:** Crítico

**Descripción**
El sistema no pide usuario ni contraseña, ni tokens, ningún método de autenticación. Cualquiera que descubra la URL del servicio puede ingresar y consultar el saldo de un cliente o, peor aún, introducir aportes falsos a nombre de cualquiera porque el código le cree ciegamente al ID que le manden. En un fondo de pensiones esto es fraude financiero seguro.

**Solución**
Implementar la librería de Spring Security para que pida un token (JWT) y poner una regla para que el sistema revise que el usuario que está logueado sea el mismo dueño de la cuenta (o un asesor autorizado).

**Ejemplo de corrección**

```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                    .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt());
        return http.build();
    }
}
```

```java
@PreAuthorize("#req.afiliadoId == authentication.name or hasRole('ASESOR')")
@PostMapping
public AporteResponse registrar(@Valid @RequestBody AporteRequest req) {
    return aporteMapper.toResponse(service.registrar(req));
}
```

---

## 3. Dinero modelado con `double`

**Riesgo:** Crítico

**Descripción**
Para los montos de dinero y los saldos se usó el tipo de dato `double`. En programación, los `double` no son exactos con los decimales y se comen centavos o los redondean mal cuando se hacen muchas sumas. Al final del mes, cuando el área de contabilidad vaya a cuadrar la caja, la suma de todos los aportes no va a coincidir con el saldo total por culpa de esos centavos perdidos.

**Solución**
Migrar todo el dominio monetario a `BigDecimal` con escala explícita y columna `NUMERIC`.

**Ejemplo de corrección**

Antes (`domain/Aporte.java`, `domain/Saldo.java`, `domain/EventoAporte.java`, `dto/AporteRequest.java`):

```java
private double monto;
private double totalMes;
```

Después:

```java
@Column(name = "monto", precision = 19, scale = 2, nullable = false)
private BigDecimal monto;

@Column(name = "total_mes", precision = 19, scale = 2, nullable = false)
private BigDecimal totalMes;
```

El mismo cambio aplica para `EventoAporte.monto`, `AporteRequest.monto` y las propiedades `aporte.tope-mensual` y `aporte.umbral-revision` de `AporteService`.

---

## 4. Control de tope mensual inoperante (`==` en vez de `>`)

**Riesgo:** Crítico

**Descripción**
El sistema debe bloquear un aporte si el cliente se pasa de su tope permitido al mes. Pero el código dice: `if (nuevo == topeMensual)`. Eso significa que solo va a fallar si el usuario aporta exactamente la misma cantidad del tope. Si se pasa por un solo peso, el sistema lo deja seguir como si nada, rompiendo la regla del negocio. Además, comparar `double` con un `==` casi nunca da verdadero por lo explicado en el hallazgo anterior.

**Solución**
Como cambiamos todo a `BigDecimal`, tenemos que usar el método `.compareTo()` para revisar si el nuevo saldo es mayor que el tope.

**Ejemplo de corrección**

Antes (`service/AporteService.java`):

```java
double nuevo = s.getTotalMes() + monto;

if (nuevo == topeMensual) {
    throw new IllegalArgumentException("El monto supera el tope mensual permitido");
}
```

Después:

```java
BigDecimal nuevo = s.getTotalMes().add(monto);

if (nuevo.compareTo(topeMensual) > 0) {
    throw new TopeMensualSuperadoException(req.getAfiliadoId(), nuevo, topeMensual);
}
```

---

## 5. Violación de capas y arquitectura

**Riesgo:** Alto

**Descripción**
El README del proyecto dice que usamos "Clean Architecture", lo que significa que el controlador solo recibe peticiones y el dominio tiene las reglas del negocio. Sin embargo, el controlador está haciendo consultas SQL directamente (por eso ocurre lo explicado en el hallazgo 1). Además, los archivos de la carpeta `domain` están llenos de cosas de Hibernate (`@Entity`), lo que significa que si mañana cambiamos de base de datos, se nos rompe el núcleo del negocio.

Aunque logremos limpiar las clases de dominio de anotaciones de Hibernate, el servicio del caso de uso (`AporteService`) sigue importando y dependiendo directamente de `AporteJpaRepository` (que es una tecnología específica de infraestructura de Spring Data). Esto rompe la regla de "Inversión de Dependencias". Si el día de mañana el equipo decide cambiar la base de datos a MongoDB o conectarse por un API externa, nos va a tocar borrar y reescribir todo el servicio de negocio, cuando se supone que las reglas de negocio deberían ser intocables e independientes de la tecnología de turno.

**Solución**
Eliminar el acceso JDBC del controlador (ver hallazgo 1) y encapsular la regla de negocio en el propio agregado. Además, debemos crear un "puerto", que es simplemente una interfaz limpia en la capa de dominio que defina qué operaciones se necesitan. Luego, en la capa de infraestructura, creamos un adaptador que implemente esa interfaz usando Spring Data JPA. El servicio ahora solo conocerá la interfaz del dominio.

**Ejemplo de corrección**

Puerto en el dominio:

```java
package co.proteccion.cis.retoa.domain.port;

public interface AporteRepository {
    Aporte guardar(Aporte aporte);
    List<Aporte> buscarPorAfiliadoYPeriodo(String afiliadoId, String periodo);
}
```

Adaptador en infraestructura:

```java
package co.proteccion.cis.retoa.infraestructura.persistencia;

@Component
@RequiredArgsConstructor
class AporteRepositoryAdapter implements AporteRepository {

    private final AporteJpaRepository jpaRepository;

    public Aporte guardar(Aporte aporte) {
        return jpaRepository.save(AporteEntityMapper.toEntity(aporte)).toDominio();
    }

    public List<Aporte> buscarPorAfiliadoYPeriodo(String afiliadoId, String periodo) {
        return jpaRepository.findByAfiliadoIdAndPeriodo(afiliadoId, periodo).stream()
                .map(AporteEntityMapper::toDominio)
                .toList();
    }
}
```

`AporteService` pasa a depender únicamente de `AporteRepository` (el puerto del dominio), nunca de `AporteJpaRepository`.

---

## 6. Entidad JPA expuesta como contrato de API, sin validación de entrada

**Riesgo:** Alto

**Descripción**
El endpoint de crear aporte devuelve directamente el objeto de la base de datos. Esto expone campos de control interno (como `marcadaRevision`, que es un flag de cumplimiento para auditoría) que el cliente final no tiene por qué ver. Además, si el cliente manda el `afiliadoId` vacío o el monto en cero, el sistema lo deja pasar y estalla más adelante en la base de datos en lugar de frenarlo de inmediato.

**Solución**
Hay que usar un objeto limpio para la respuesta (un DTO o `record` que solo tenga lo necesario) y ponerle anotaciones como `@NotBlank` o `@DecimalMin` a los datos que entran para que Spring los valide antes de procesarlos.

**Ejemplo de corrección**

Antes (`dto/AporteRequest.java` y `controller/AporteController.java`):

```java
public class AporteRequest {
    private String afiliadoId;
    private double monto;
    private String canal;
}
```

```java
@PostMapping
public Aporte registrar(@RequestBody AporteRequest req) {
    return service.registrar(req);
}
```

Después:

```java
public record AporteRequest(
        @NotBlank String afiliadoId,
        @DecimalMin(value = "0.01") BigDecimal monto,
        @NotBlank String canal) {
}

public record AporteResponse(
        Long id,
        String afiliadoId,
        BigDecimal monto,
        LocalDate fecha,
        String canal,
        String periodo) {
}
```

```java
@PostMapping
public AporteResponse registrar(@Valid @RequestBody AporteRequest req) {
    return aporteMapper.toResponse(service.registrar(req));
}
```

`marcadaRevision` deja de viajar en la respuesta; queda únicamente como dato interno de auditoría.

---

## 7. Trazabilidad de auditoría insuficiente en `EventoAporte`

**Riesgo:** Alto

**Descripción**
Por ley, tenemos que guardar un historial de auditoría muy estricto de los movimientos. El código intenta hacerlo guardando un `EventoAporte`, pero crea el evento antes de guardar el aporte en la base de datos. Como el aporte aún no tiene un ID generado, el evento se guarda apuntando a un ID vacío (`null`). Si un auditor nos pide revisar un movimiento sospechoso, va a ser imposible saber qué aporte generó ese evento.

**Solución**
Primero debemos guardar el aporte para que la base de datos le asigne su ID, y luego guardamos el evento pasándole ese ID real y el saldo que tenía antes y después.

**Ejemplo de corrección**

Antes (`service/AporteService.java`):

```java
eventoRepo.save(new EventoAporte(aporte));

log.info("Aporte registrado: monto={} afiliado={}", monto, req.getAfiliadoId());

return aporteRepo.save(aporte);
```

Después:

```java
BigDecimal saldoAnterior = s.getTotalMes();
BigDecimal saldoPosterior = saldoAnterior.add(monto);

Aporte aporteGuardado = aporteRepo.save(aporte);

eventoRepo.save(new EventoAporte(
        aporteGuardado.getId(),
        aporteGuardado.getAfiliadoId(),
        aporteGuardado.getMonto(),
        saldoAnterior,
        saldoPosterior));

return aporteGuardado;
```

`EventoAporte` se amplía para recibir el `aporteId`, el `saldoAnterior` y el `saldoPosterior` en su constructor.

---

## 8. Condición de carrera y falta de atomicidad transaccional

**Riesgo:** Crítico

**Descripción**
El método para registrar aportes no tiene la anotación `@Transactional` ni ningún tipo de bloqueo. Funciona con una lógica muy peligrosa: lee el saldo de la base de datos, calcula el nuevo valor sumando en la memoria del servidor y luego guarda el resultado. Si un cliente le da al botón "Aportar" dos veces seguidas muy rápido por culpa de una interrupción en su celular, se procesarán los dos intentos al mismo tiempo. Ambos van a leer el mismo saldo inicial (por ejemplo, $100), ambos calcularán que el nuevo saldo es $150, y el último en guardar pisará al otro. Al final, el cliente habrá aportado $100 reales, pero en su saldo solo se verán reflejados $50. Se pierde plata en el acumulado. Además, si el sistema llega a fallar a mitad del proceso, podría alcanzar a actualizar el saldo pero no crear el recibo del aporte, dejando datos "huérfanos" y descuadrados.

**Solución**
Hay que volver el método completamente atómico poniéndole `@Transactional` para que si algo falla se borre todo, y además aplicar un "bloqueo de escritura" (lock pesimista) en el repositorio. Así, cuando entre el primer clic, el sistema congela esa fila en la base de datos y obliga al segundo clic a esperar en fila hasta que el primero termine por completo.

**Ejemplo de corrección**

Antes (`service/AporteService.java`):

```java
public Aporte registrar(AporteRequest req) {
    ...
}
```

Después:

```java
@Transactional
public Aporte registrar(AporteRequest req) {
    ...
}
```

```java
// repository/SaldoJpaRepository.java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<Saldo> findByAfiliadoId(String afiliadoId);
```

---

## 9. No hay protección contra reintentos (falta de idempotencia)

**Riesgo:** Alto

**Descripción**
No hay ninguna validación que impida que una misma transacción se procese dos veces. Si la aplicación móvil del cliente envía un aporte y la red se cae justo antes de recibir la confirmación, lo normal es que la app vuelva a mandar la petición por seguridad. Como el sistema no lleva un registro de qué peticiones ya procesó, tomará el reintento como un aporte nuevo y legítimo, cobrándole doble dinero real al afiliado en su cuenta bancaria.

**Solución**
Debemos exigirle obligatoriamente al frontend un identificador único por cada operación en los encabezados (una `Idempotency-Key`). Antes de crear cualquier aporte, el servicio debe revisar dentro de la transacción si esa clave ya existe en la base de datos; si ya existe, simplemente devuelve el recibo que se había guardado la primera vez en lugar de volver a cobrar y meter más plata.

**Ejemplo de corrección**

```java
@PostMapping
public AporteResponse registrar(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                 @Valid @RequestBody AporteRequest req) {
    return aporteMapper.toResponse(service.registrar(req, idempotencyKey));
}
```

```java
@Transactional
public Aporte registrar(AporteRequest req, String idempotencyKey) {
    return aporteRepo.findByIdempotencyKey(idempotencyKey)
            .orElseGet(() -> procesarNuevoAporte(req, idempotencyKey));
}
```

---

## 10. Sin manejo global de errores — códigos HTTP incorrectos

**Riesgo:** Medio

**Descripción**
Como en el proyecto no existe un controlador global de errores (`@ControllerAdvice`), cuando pasa algo normal del negocio (por ejemplo, que el monto sea inválido o que el afiliado no exista), el sistema lanza una excepción genérica que Spring Boot no sabe cómo interpretar. Al final, el sistema se confunde, piensa que el servidor se cayó y le responde al cliente con un feo código `500 Internal Server Error`. Esto ensucia las métricas de monitoreo de la célula (activando alertas falsas para el equipo que está de soporte), oculta al usuario qué hizo mal y, si tenemos activa la opción de incluir mensajes detallados, podríamos terminar enseñando pedazos de código internos que faciliten un hackeo.

**Solución**
Hay que implementar una clase centralizada para capturar los errores. Si es un error del cliente (como pasarse del tope o mandar datos inválidos), respondemos con un código correcto de error de cliente (`400 Bad Request` o `404 Not Found`). Y si ocurre un error inesperado de verdad (un error `500`), le ocultamos el detalle técnico al usuario y solo le mostramos un código de referencia (`correlationId`) para que soporte lo busque internamente.

**Ejemplo de corrección**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> manejarSolicitudInvalida(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(AfiliadoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> manejarAfiliadoNoEncontrado(AfiliadoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarErrorInesperado(Exception ex) {
        String correlationId = UUID.randomUUID().toString();
        log.error("Error inesperado [correlationId={}]", correlationId, ex);
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse("Error interno. Referencia: " + correlationId));
    }
}
```

---

## 11. Ausencia real de separación CQRS

**Riesgo:** Medio

**Descripción**
En la documentación y el README del proyecto se especifica el uso del patrón CQRS (que sirve para separar de forma estricta las operaciones que modifican datos de las operaciones que solo leen información). Sin embargo, al ir a mirar el código real, el comando (guardar un aporte) y la consulta (ver el consolidado) están metidos a la fuerza en el mismo controlador, usando los mismos modelos. Un revisor o arquitecto de otra célula nos bloquearía el despliegue de inmediato por documentar una cosa y codificar otra completamente distinta.

**Solución**
Hay que separar las responsabilidades creando controladores y manejadores independientes para cada tipo de acción, cada uno con sus propios objetos de datos (DTO) de entrada y salida: uno exclusivo para la escritura (`AporteCommandController`) y otro dedicado únicamente a las consultas (`AporteQueryController`).

**Ejemplo de corrección**

```java
@RestController
@RequestMapping("/api/aportes")
@RequiredArgsConstructor
public class AporteCommandController {

    private final AporteCommandHandler handler;

    @PostMapping
    public AporteResponse registrar(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                     @Valid @RequestBody AporteRequest req) {
        return handler.ejecutar(req, idempotencyKey);
    }
}
```

```java
@RestController
@RequestMapping("/api/aportes")
@RequiredArgsConstructor
public class AporteQueryController {

    private final AporteQueryHandler handler;

    @GetMapping("/consolidado")
    public List<AporteResponse> consolidado(@RequestParam String afiliadoId,
                                            @RequestParam String periodo) {
        return handler.consultar(afiliadoId, periodo);
    }
}
```

---

## 12. Datos privados de clientes guardados en los registros de texto (logs expuestos)

**Riesgo:** Medio

**Descripción**
El código tiene una línea que escribe en la consola: `log.info("Aporte registrado: monto={} afiliado={}", ...)`. Esto guarda directamente el documento del afiliado y el dinero que está metiendo en texto claro. Cualquier operador, administrador de sistemas o desarrollador con acceso a las herramientas de monitoreo (como Splunk) va a poder ver la información financiera privada de los clientes sin ningún permiso.

**Solución**
Los datos exactos de la transacción financiera solo deben vivir dentro de la base de datos protegida (en la tabla `EventoAporte`), la cual tiene cifrado y accesos muy restringidos. En los registros comunes de la aplicación solo debemos escribir cosas anónimas o usar un "hash" (una firma oculta) para poder rastrear sin revelar la identidad.

**Ejemplo de corrección**

Antes (`service/AporteService.java`):

```java
log.info("Aporte registrado: monto={} afiliado={}", monto, req.getAfiliadoId());
```

Después:

```java
log.info("Aporte registrado: afiliadoHash={} canal={}",
        HashUtil.sha256(req.getAfiliadoId()), req.getCanal());
```

---

## (EXTRA, EN ENTORNO DE PRODUCCIÓN) Persistencia volátil — sin perfiles para producción

**Riesgo:** Alto

**Descripción**
El proyecto está configurado para usar una base de datos en memoria (H2) que se crea y se destruye desde cero cada vez que la aplicación se apaga o se enciende (`ddl-auto=create-drop`). Además, es el único archivo de configuración que hay en todo el código.

Si subimos esto a Kubernetes en producción, cada vez que el sistema haga un despliegue, se reinicie un contenedor o falle la red, toda la plata, los saldos y el histórico de aportes de los clientes se van a borrar para siempre. No hay forma de que un fondo regulado sobreviva con esto. Para colmo, la consola web de H2 está prendida para cualquiera, lo que significa que si alguien descubre la URL, podría entrar a mirar las tablas desde el navegador.

**Solución**
Hay que prohibir las bases de datos en memoria para producción. Debemos migrar el archivo a un formato `application.yml` estructurado por perfiles. Por defecto correrá el perfil de desarrollo (`dev`), pero crearemos un bloque exclusivo para el perfil de producción (`prod`) que apunte a nuestra base de datos real (PostgreSQL), use variables de entorno ocultas para las contraseñas, valide la estructura con Flyway y apague por completo la consola H2.

**Ejemplo de corrección**

Antes (`application.properties`, único archivo, sin perfiles):

```properties
spring.datasource.url=jdbc:h2:mem:retodb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.jpa.hibernate.ddl-auto=create-drop
spring.h2.console.enabled=true
```

Después (`application.yml`, separado por perfiles):

```yaml
spring:
  profiles:
    active: dev

---
spring:
  config:
    activate:
      on-profile: dev
  datasource:
    url: jdbc:h2:mem:retodb
  jpa:
    hibernate:
      ddl-auto: create-drop
  h2:
    console:
      enabled: true

---
spring:
  config:
    activate:
      on-profile: prod
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
  h2:
    console:
      enabled: false
```

---
## Veredicto del MR

**Estado:** BLOQUEADO

**Motivo:** El código no puede subir a producción bajo ninguna circunstancia en su estado actual. Tiene fallas críticas de seguridad (inyección SQL directa y ausencia total de autenticación), errores contables severos por usar `double` para montos, lógica rota que permite saltarse el tope mensual y una condición de carrera que puede duplicar o perder dinero real de los clientes con dos clics seguidos. Aunque el pipeline esté en verde y los tests locales pasen, la suite actual no prueba escenarios de estrés, caminos de error ni concurrencia, por lo que aprobar este Merge Request solo porque "compila" sería una irresponsabilidad grave en un módulo que mueve dinero real en un entorno financiero regulado. El servicio necesita una reestructuración profunda en los puntos críticos antes de poder volver a ser evaluado.
