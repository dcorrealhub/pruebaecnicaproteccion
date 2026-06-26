# Entregable Técnico — Reto B: Fondo de Aportes Voluntarios

**CIS Protección S.A. — Candidato: Andrés Giraldo**

---

## 1. Resumen ejecutivo

Se implementó el dominio completo de **aportes voluntarios** sobre el scaffold de CIS Protección S.A., aplicando arquitectura hexagonal (Ports & Adapters), con capa de dominio libre de dependencias de infraestructura, persistencia correcta en PostgreSQL y una suite de tests unitarios que cubre los caminos críticos del negocio.

---

## 2. Arquitectura implementada

```
┌─────────────────────────────────────────────────────────────┐
│  Infrastructure                                             │
│  ┌────────────────┐   ┌──────────────────────────────────┐  │
│  │  Web (REST)    │   │  Persistence (JPA / Flyway)      │  │
│  │  Controllers   │   │  Entities, Adapters, Migrations  │  │
│  │  DTOs          │   │  SpringData Repositories         │  │
│  └───────┬────────┘   └──────────────────┬───────────────┘  │
│          │ Ports IN                       │ Ports OUT        │
│  ────────┼────────── Application ─────────┼─────────────     │
│          │           Use Cases            │                  │
│  ────────┼────────── Domain ──────────────┼─────────────     │
│          │           Models, Exceptions   │                  │
│          └───────────────────────────────┘                  │
└─────────────────────────────────────────────────────────────┘
```

### Entidades de dominio

| Entidad | Descripción |
|---|---|
| `Afiliado` | Titular del fondo. Estados: `ACTIVO`, `BLOQUEADO` |
| `Aporte` | Transacción voluntaria. Ciclo de vida con máquina de estados |
| `ParametrosFondo` | Configuración dinámica: `montoMinimo`, `umbralRevision`, `topeMensual` |
| `SaldoMensual` | Acumulado mensual por afiliado. Control de concurrencia optimista (`@Version`) |
| `RevisionAporte` | Auditoría inmutable de cada cambio de estado |

---

## 3. Ciclo de vida del aporte

```
                 POST /api/aportes
                        │
               ┌────────▼─────────┐
               │    PENDIENTE     │ ◄── monto ≤ umbralRevision
               └──────┬───────────┘
                      │
          ┌───────────┼──────────────┐
          │           │              │
          ▼           ▼              ▼
   EN_REVISION    APROBADO       ANULADO
   (monto >    (directa desde   (solo afiliado
    umbral)      PENDIENTE)      titular, libera
          │                       cupo mensual)
          │
    ┌─────┴──────┐
    ▼            ▼
 APROBADO    RECHAZADO
              (libera cupo
               mensual)
```

Estados terminales: `APROBADO`, `RECHAZADO`, `ANULADO`

---

## 4. API REST — Endpoints documentados (Swagger UI: `/swagger-ui.html`)

### Afiliados — `/api/afiliados`

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/afiliados` | Registrar afiliado (estado inicial: ACTIVO) |
| `GET` | `/api/afiliados` | Listar todos los afiliados |
| `GET` | `/api/afiliados/{afiliadoId}` | Consultar afiliado por ID sintético |

### Aportes — `/api/aportes`

| Método | Ruta | Actor | Descripción |
|---|---|---|---|
| `POST` | `/api/aportes` | Afiliado | Registrar aporte (idempotente por `idempotenciaKey`) |
| `GET` | `/api/aportes/consolidado` | Revisor | Consolidado por periodo `YYYY-MM` |
| `PATCH` | `/api/aportes/{id}/estado` | Revisor | Cambiar estado (aprobación / rechazo) |
| `PATCH` | `/api/aportes/{id}/anular` | Afiliado | Anular aporte propio en estado PENDIENTE |
| `GET` | `/api/aportes/{id}/revisiones` | Revisor | Historial de revisiones de un aporte |

### Parámetros — `/api/parametros`

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/parametros/actual` | Parámetros vigentes |
| `GET` | `/api/parametros/historial` | Historial auditado de cambios |
| `POST` | `/api/parametros` | Actualizar topes (efecto inmediato, sin reinicio) |

---

## 5. Reglas de negocio implementadas

### 5.1 Validaciones al registrar un aporte

| # | Regla | Excepción | HTTP |
|---|---|---|---|
| R1 | El afiliado debe existir | `AfiliadoNotFoundException` | 404 |
| R2 | El afiliado debe estar en estado **ACTIVO** | `AfiliadoBloqueadoException` | 422 |
| R3 | El monto debe ser ≥ `montoMinimo` configurado | `MontoMinimoNoAlcanzadoException` | 422 |
| R4 | El acumulado mensual no puede superar `topeMensual` | `TopeMensualExcedidoException` | 422 |
| R5 | Idempotencia por `idempotenciaKey` — reenvíos retornan el aporte original sin efecto secundario | — | 201 |
| R6 | Si `monto > umbralRevision` → estado automático `EN_REVISION`; si no → `PENDIENTE` | — | 201 |

### 5.2 Validaciones al cambiar estado (revisor)

| # | Regla | Excepción | HTTP |
|---|---|---|---|
| R7 | Solo se permiten transiciones válidas definidas en `EstadoAporte` | `TransicionEstadoInvalidaException` | 400 |
| R8 | Al **RECHAZAR**: liberar el cupo mensual (decrementar `SaldoMensual`) | — | 200 |
| R9 | El revisor es campo obligatorio y auditable | Bean Validation | 400 |
| R10 | Toda transición queda registrada en `revision_aporte` con timestamp | — | — |

### 5.3 Anulación por el afiliado

| # | Regla | Excepción | HTTP |
|---|---|---|---|
| R11 | Solo el afiliado titular puede anular su propio aporte | `AporteNoAutorizadoException` | 403 |
| R12 | Solo se puede anular desde estado **PENDIENTE** | `TransicionEstadoInvalidaException` | 400 |
| R13 | Al anular, se libera el cupo mensual | — | 200 |

### 5.4 Gestión de parámetros

| # | Regla |
|---|---|
| R14 | Invariante: `montoMinimo` < `umbralRevision` < `topeMensual` |
| R15 | Cambio de parámetros es inmediato y auditado (append-only en `historico_parametros`) |
| R16 | Al arrancar sin parámetros en DB, se siembran desde variables de entorno |

### 5.5 Concurrencia

| # | Regla |
|---|---|
| R17 | `SaldoMensual` usa `@Version` (control optimista) — evita race conditions en aportes simultáneos |
| R18 | La inicialización de `SaldoMensual` usa `REQUIRES_NEW` para aislar la creación de la transacción principal |

---

## 6. Migraciones de base de datos

| Versión | Descripción |
|---|---|
| V1 | Esquema base: `aporte`, `saldo_mensual`, `evento_aporte` |
| V2 | Campo `estado` VARCHAR(20) en `aporte` |
| V3 | Tabla `afiliado` con `estado_afiliado` |
| V4 | Tabla `historico_parametros` |
| V5 | Tabla `revision_aporte` con FK a `aporte` |
| V6 | Migración de PKs BIGSERIAL → UUID |
| **V7** | **Campo `monto_minimo` en `historico_parametros`** |

---

## 7. Cobertura de tests (92 tests, 0 fallos)

| Clase de test | Casos cubiertos |
|---|---|
| `EstadoAporteTest` | Todas las transiciones válidas e inválidas incluyendo ANULADO |
| `RegistrarAporteUseCaseImplTest` | Idempotencia, afiliado no encontrado, afiliado bloqueado, monto mínimo, tope mensual, estado automático por umbral, precedencia parámetros DB |
| `CambiarEstadoAporteUseCaseImplTest` | Transiciones válidas, liberación de saldo al rechazar, sin saldo no falla, datos de revisión correctos, estados terminales |
| `AnularAporteUseCaseImplTest` | Not found, no titular, EN_REVISION no anulable, APROBADO no anulable, camino feliz, saldo decrementado, revisión registrada, sin saldo no falla |
| `ActualizarParametrosUseCaseImplTest` | Invariante triple (montoMinimo/umbral/tope), persistencia correcta, datos auditables |
| `AporteControllerTest` | Registro, consolidado, cambio de estado, revisiones (capa HTTP) |
| `ParametrosControllerTest` | Consulta actual/historial, actualización, validaciones DTO incluyendo montoMinimo |
| `AfiliadoControllerTest` | Registro, consulta, duplicados |

---

## 8. Mejoras futuras identificadas

### 8.1 Corto plazo (próxima iteración)

**a) Límite de aportes por período (no solo monto)**
Un afiliado podría enviar 500 aportes de $10.000 sin problema. Agregar `maxAportesPorPeriodo` en `ParametrosFondo` protege contra errores de integración y abuso.

```java
// ParametrosFondo
private final Integer maxAportesPorPeriodo; // ej: 5

// RegistrarAporteUseCaseImpl
long conteo = aporteRepository.countByAfiliadoIdAndPeriodo(afiliadoId, periodo);
if (conteo >= params.getMaxAportesPorPeriodo()) {
    throw new LimiteAportesPorPeriodoException(...);
}
```

**b) Restricción de aporte duplicado en revisión activa**
Si el afiliado tiene un aporte `EN_REVISION` para el mismo periodo, bloquear nuevos registros hasta resolución. Evita acumulación de aportes pendientes de revisión por el mismo concepto.

**c) Test de integración con Testcontainers**
Los tests actuales son todos unitarios con Mockito. Un test de integración con PostgreSQL real validaría:
- La migration V7 corre sin errores en un esquema limpio
- La idempotencia funciona a nivel de constraint de DB (`UNIQUE (idempotencia_key)`)
- El control optimista de versión realmente falla en concurrencia

### 8.2 Mediano plazo

**d) Domain Events para desacoplar side effects**
`CambiarEstadoAporteUseCaseImpl` mezcla la transición de estado con la liberación del saldo en la misma transacción. Publicar eventos de dominio (`AporteRechazadoEvent`, `AporteAprobadoEvent`) via `ApplicationEventPublisher` permite añadir side effects (notificaciones, reportes) sin tocar los use cases.

```java
// En CambiarEstadoAporteUseCaseImpl
eventPublisher.publishEvent(new AporteRechazadoEvent(guardado));

// Listener independiente
@EventListener
@TransactionalEventListener(phase = AFTER_COMMIT)
void onRechazo(AporteRechazadoEvent event) { ... }
```

**e) Bloqueo automático por rechazos consecutivos**
Si un afiliado acumula N rechazos consecutivos en un período, transicionar automáticamente a `EstadoAfiliado.BLOQUEADO`. Requiere contador en `Afiliado` o consulta al historial de revisiones.

**f) Auto-expiración de aportes PENDIENTES**
Aportes que llevan más de X días en `PENDIENTE` sin ser revisados deberían pasar a `EN_REVISION` o ser notificados. Implementable con un `@Scheduled` job.

---

## 9. Integraciones externas recomendadas

### 9.1 SARLAFT / LISTAS RESTRICTIVAS

El **Sistema de Administración del Riesgo de Lavado de Activos y de la Financiación del Terrorismo (SARLAFT)** es de obligatorio cumplimiento para fondos de inversión y pensiones en Colombia (Circular Externa 026/2008 SFC).

**Punto de integración natural:** al momento de registrar el aporte, **antes de persistir**, verificar al afiliado en listas restrictivas.

```
POST /api/aportes
    │
    ├── Validar afiliado ACTIVO
    ├── Validar montoMinimo
    ├─► [NUEVO] Consultar SARLAFT / listas ONU / OFAC
    │       Si coincidencia → lanzar AporteRestringidoException (HTTP 422)
    │       Si OK → continuar
    ├── Aplicar tope mensual
    └── Persistir aporte
```

**Implementación sugerida (puerto de salida):**

```java
// domain/port/out/VerificacionRiesgoPort.java
public interface VerificacionRiesgoPort {
    ResultadoVerificacion verificar(String afiliadoId, BigDecimal monto);
}

public record ResultadoVerificacion(boolean aprobado, String codigoRiesgo, String detalle) {}
```

```java
// infrastructure/integration/SarlaftAdapter.java
@Component
public class SarlaftAdapter implements VerificacionRiesgoPort {
    // Consulta al servicio SARLAFT institucional vía REST/SOAP
    // Verifica: listas ONU, OFAC, PEP (Personas Expuestas Políticamente)
    // Si el servicio no responde → política configurable: rechazar o permitir con alerta
}
```

**Criterios de verificación típicos:**
- Listas internacionales: ONU, OFAC (EE.UU.), UE
- PEP (Personas Expuestas Políticamente)
- Lista Clinton / FCPA
- Listas locales UIAF (Colombia)
- Monto de la transacción (umbrales de reporte automático)

**Consideraciones de diseño:**
- El resultado debe quedar en `revision_aporte` como trazabilidad
- Si el servicio SARLAFT no responde, la política puede ser: rechazar el aporte (`fail-closed`) o aceptar con bandera de revisión manual (`fail-open`)
- Los registros positivos (coincidencias) deben notificarse a la UIAF en el formato ROS (Reporte de Operación Sospechosa)

### 9.2 Notificaciones (email / push / SMS)

Eventos que ameritan notificación al afiliado:

| Evento | Canal sugerido |
|---|---|
| Aporte registrado exitosamente | Email + Push |
| Aporte aprobado | Email + Push |
| Aporte rechazado (con motivo) | Email |
| Aporte anulado | Email |
| Afiliado bloqueado | Email + notificación admin |
| Tope mensual próximo a agotarse (80%) | Push preventiva |

Implementación: puerto de salida `NotificacionPort` con adaptador a SendGrid/AWS SES para email y Firebase para push.

### 9.3 Centralización de parámetros

Hoy `ParametrosFondo` se persiste en la DB del microservicio. Para entornos multi-instancia o multi-microservicio podría externalizarse a **Spring Cloud Config** o **AWS Parameter Store**, con actualización vía `@RefreshScope` sin reinicio de pods.

### 9.4 Auditoría centralizada

La tabla `revision_aporte` es auditoría local. En un ecosistema enterprise se complementaría con:
- **Kafka / RabbitMQ**: publicar eventos de dominio a un topic de auditoría
- **OpenSearch / ELK**: indexar eventos para consultas operativas y alertas en tiempo real
- **Compliance reporting**: extraer reportes ROS para la UIAF directamente del bus de eventos

---

## 10. Variables de entorno

| Variable | Default | Descripción |
|---|---|---|
| `APORTE_MONTO_MINIMO` | `10000` | Monto mínimo aceptado por aporte (COP) |
| `APORTE_UMBRAL_REVISION` | `5000000` | Umbral que dispara revisión manual (COP) |
| `APORTE_TOPE_MENSUAL` | `10000000` | Tope mensual por afiliado (COP) |
| `SPRING_DATASOURCE_URL` | — | URL JDBC PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | — | Usuario DB |
| `SPRING_DATASOURCE_PASSWORD` | — | Contraseña DB |

---

## 11. Ejecución

```bash
# Con Docker Compose (recomendado)
docker compose up --build

# Swagger UI
http://localhost:8082/swagger-ui.html

# Local con DB externa
JAVA_HOME=/usr/lib/jvm/java-21-openjdk mvn spring-boot:run \
  -Dspring-boot.run.profiles=local

# Tests
mvn test
# → 92 tests, 0 failures
```

---

*Documento generado como parte de la prueba técnica — rama `candidato/andres-giraldo`*
