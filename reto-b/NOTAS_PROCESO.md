# Reto B — Notas de proceso

## 1. Cómo se trabajó con la IA

Herramienta: Claude Code (agente con acceso al repositorio, terminal y navegador).

**Prompt inicial:**
> "En el pdf que te pasé está una prueba técnica que tengo que resolver, necesito que me hagas el reto B con las instrucciones del PDF y escribiendo código sobre el zip que te pasé. Si tienes dudas antes de empezar con el código me avisas."

**Preparación del contexto:** antes de escribir código el agente leyó el brief completo, el README del repositorio
y todo el código base del reto. Con eso armó una lista de ambigüedades y la presentó para decidir **antes** de implementar.

**Preguntas de aclaración y decisiones tomadas:**

| Pregunta | Decisión | Motivo |
|---|---|---|
| ¿Quién define la fecha del aporte? | **El servidor** (`Clock` en zona `America/Bogota`) | El cliente no puede retrofechar aportes para esquivar el tope de un mes. |
| ¿Misma `idempotenciaKey` con datos distintos? | **409 Conflict** | Es un error del cliente; devolver el original en silencio ocultaría un bug. |
| ¿Límites inclusivos o exclusivos? | Tope: `acumulado + monto > tope` → rechazo (llegar exacto es válido). Umbral: `monto > umbral` → revisión | El brief dice "supere". Los aportes marcados **sí** cuentan para el tope. |
| ¿"Tope mensual por afiliado" es uno global o uno por afiliado? | **Cada afiliado tiene su propio tope** en la tabla `parametro_afiliado`. Si un afiliado no tiene, aplica `aporte.tope-mensual` | Primera versión: un tope global aplicado al acumulado de cada afiliado. Al releer el brief con el candidato se corrigió (ver abajo). |
| ¿El umbral de revisión también es por afiliado? | **Sí** (ajuste de requisito pedido por el candidato). Columna `umbral_revision` en `parametro_afiliado`; si es NULL aplica `aporte.umbral-revision` | Mismo patrón que el tope. Cada parámetro cae a su valor por defecto de forma independiente. |
| Negocio: SUCURSAL tiene mayor riesgo y se marca sobre 3.000.000. ¿Cómo se combina con el umbral del afiliado? | **Manda el del canal**: en SUCURSAL siempre 3.000.000, aunque el afiliado tenga un umbral propio. En los demás canales aplica el umbral del afiliado o, si no tiene, 5.000.000 | Decisión del candidato ante el ajuste de requisito. El umbral por canal es configurable (`aporte.umbral-revision-por-canal.SUCURSAL`). |

### Corrección de la interpretación del tope y ajuste del umbral

La primera implementación leyó "existe un tope mensual por afiliado (parámetro configurable)" como **un solo valor**
en `application.properties` aplicado al acumulado de cada afiliado. Al revisarlo, el candidato señaló que el tope es
**de cada afiliado**. Después pidió que el **umbral de revisión** también fuera por afiliado. Así quedó:

- `V3__tope_por_afiliado.sql` creó `tope_afiliado`. `V4__parametros_por_afiliado.sql` la renombra a **`parametro_afiliado`**
  y agrega `umbral_revision`. Se renombra en vez de crear otra tabla para conservar los topes ya cargados y para
  tener todos los parámetros de un afiliado en una sola fila. Ambas columnas aceptan NULL y tienen un `CHECK (> 0)`.
- El dominio recibe un `ParametrosAfiliado(topeMensual, umbralRevision)` a través del puerto `ParametrosAfiliadoRepositoryPort`
  y no sabe de dónde salen los valores.
- `PoliticaAportes.topeAplicable(...)` y `umbralAplicable(...)` devuelven el valor propio o el valor por defecto.
  `validarTopeMensual` y `requiereRevision` reciben el valor ya resuelto.
- El cambio no tocó las reglas (`acumulado + monto > tope`, `monto > umbral`), ni el control de concurrencia, ni el
  contrato HTTP. Eso valida haber aislado las reglas en el dominio.

Para configurar los parámetros de un afiliado (datos sintéticos; NULL = usar el valor por defecto):

```sql
INSERT INTO parametro_afiliado (afiliado_id, tope_mensual, umbral_revision) VALUES ('AF-001', 2000000, 300000)
ON CONFLICT (afiliado_id) DO UPDATE
    SET tope_mensual = EXCLUDED.tope_mensual, umbral_revision = EXCLUDED.umbral_revision, actualizado_en = NOW();
```

### Ajuste de requisito: umbral por canal (SUCURSAL)

> "Los aportes por sucursal tienen mayor riesgo. Un aporte por canal SUCURSAL se marca para revisión si supera
> 3.000.000. Los demás canales siguen con 5.000.000."

Antes de implementar se preguntó cómo se combina con el umbral por afiliado. La decisión fue que **manda el del canal**.
El umbral se resuelve en este orden: **umbral del canal → umbral propio del afiliado → umbral por defecto**.

| Canal | Afiliado sin umbral propio | Afiliado con umbral propio de 8.000.000 |
|---|---|---|
| SUCURSAL | se marca si supera 3.000.000 | se marca si supera 3.000.000 (manda el canal) |
| APP_MOVIL / WEB | se marca si supera 5.000.000 | se marca si supera 8.000.000 |

- Configuración: `aporte.umbral-revision-por-canal.SUCURSAL=3000000` (un `Map<Canal, BigDecimal>` validado al arrancar).
  Si mañana otro canal se vuelve de riesgo, se agrega una línea de configuración, sin tocar código.
- Solo cambiaron `PoliticaAportes.umbralAplicable(parametros, canal)` y su configuración. Ni el caso de uso
  (solo pasa el canal), ni la persistencia, ni la API, ni el frontend cambiaron de contrato.
- Pruebas: borde exacto en SUCURSAL (3.000.000 no se marca, 3.000.000,01 sí), el mismo monto por WEB no se marca,
  y el canal prevalece sobre el umbral del afiliado. Verificado también en el despliegue de AWS.
- Pendiente para producción: el cambio de umbral no es retroactivo. Los aportes ya registrados conservan su marca,
  y faltaría registrar qué umbral se aplicó a cada aporte, para trazabilidad ante la SFC.

## 2. Diseño

```
web (controller, DTOs, ProblemDetail)  →  domain.port.in  ←  application.usecase
                                                              ↓
                       domain (Aporte, SaldoMensual, PoliticaAportes, excepciones)
                                                              ↓
                       domain.port.out  ←  infrastructure.persistence (JPA adapters)
```

- **CQRS ligero:** `RegistrarAporteUseCase` (comando, `@Transactional`) y `ConsultarAportesUseCase` (consulta, `readOnly`) están separados. El comando solo devuelve el aporte y si se creó o era un reintento.
- **Reglas en el dominio:** `PoliticaAportes` no depende de Spring y se prueba de forma aislada.
- **Concurrencia del tope:** se usa bloqueo **optimista** sobre `saldo_mensual.version`. Si dos aportes simultáneos del mismo afiliado y mes compiten, uno falla con 409 `CONFLICTO_CONCURRENCIA` y se revierte toda la transacción (saldo, aporte y evento). Como el cliente reintenta con la misma clave, el reintento es seguro. El frontend reintenta automáticamente hasta 2 veces.
- **Contrato HTTP:**

| Situación | Status | `codigo` |
|---|---|---|
| Aporte creado | 201 | – |
| Reintento idempotente (misma clave, mismos datos) | 200 | – |
| Validación de forma (campos, formato) | 400 | `VALIDACION`, `CANAL_INVALIDO`, `PERIODO_INVALIDO`, `CUERPO_INVALIDO` |
| Misma clave con otros datos | 409 | `IDEMPOTENCIA_CONFLICTO` |
| Conflicto concurrente (reintentable, `Retry-After: 1`) | 409 | `CONFLICTO_CONCURRENCIA` |
| Regla de negocio (monto, tope) | 422 | `MONTO_INVALIDO`, `TOPE_MENSUAL_EXCEDIDO` |
| Error no controlado (sin traza al cliente) | 500 | `ERROR_INTERNO` |

## 3. Pruebas (43, todas en verde)

- `PoliticaAportesTest`: bordes del tope (exacto se permite, +0,01 se rechaza), del umbral (igual no se marca), montos no positivos y más de 2 decimales.
- `RegistrarAporteUseCaseImplTest`: orquestación con puertos en memoria y reloj fijo. Cubre idempotencia, conflicto de clave, tope acumulado, tope y umbral propios del afiliado contra los valores por defecto y el periodo calculado en hora de Colombia (las 03:00 UTC del 1 de octubre siguen siendo septiembre en Bogotá).
- `AporteIntegrationTest`: PostgreSQL 15 real (Testcontainers) con las migraciones de Flyway:
  - contrato HTTP completo (201/200/400/409/422) y consolidado;
  - **8 aportes concurrentes de 2.000.000 con tope de 10.000.000: entran exactamente 5** y el saldo coincide con la suma de los aportes;
  - la misma clave enviada 6 veces en paralelo registra un solo aporte.

Se usó PostgreSQL real y no H2 porque el bloqueo de filas, las restricciones y la concurrencia son justamente lo que se quiere probar, y H2 no los reproduce igual.

También se verificó a mano en el navegador: registro, rechazo por tope con el disponible exacto, marca de revisión y consolidado.

## 4. Qué quedó por fuera a propósito y qué falta para producción (SFC)

- **Autenticación y autorización:** hoy cualquiera puede consultar cualquier `afiliadoId` (riesgo OWASP A01). En producción el afiliado sale del token (OAuth2/JWT) y no del request; la consulta también lo requiere.
- **Datos personales en logs:** los logs solo registran `aporteId`, periodo y la marca de revisión, nunca montos ni el afiliado. Falta formalizar la política de enmascaramiento y la retención.
- **Idempotencia a escala:** la clave es única a nivel global. Debería tener un alcance por cliente/afiliado y una expiración, y guardar un hash del payload en vez de comparar campos.
- **Contención:** el bloqueo optimista funciona bien con pocos aportes simultáneos por afiliado, que es el caso real. Con alta contención convendría `SELECT … FOR UPDATE` o `INSERT … ON CONFLICT` para el saldo, o reintentos en el servidor.
- **Eventos:** `evento_aporte` es una tabla de auditoría. Para integrarse con otros sistemas haría falta un outbox con publicador (Kafka/SQS).
- **Observabilidad:** faltan Actuator, métricas (aportes registrados, rechazados por regla y conflictos), trazas con correlation-id y alertas.
- **Administración de parámetros:** hoy el tope y el umbral de un afiliado se configuran por SQL. Falta un endpoint de administración con rol propio y un histórico de cambios (quién lo cambió, cuándo, valor anterior y vigencia). En un entorno SFC hay que poder responder cuál era el tope o el umbral vigente cuando se aceptó, rechazó o marcó un aporte.
- **Flujo de revisión:** se marcan los aportes, pero no existe una cola ni una pantalla para que alguien los revise y los apruebe.
- **Límites de consulta:** se debería limitar el rango de periodos y paginar el detalle.
- **Credenciales:** ya se pueden sobrescribir con `DB_URL`, `DB_USER` y `DB_PASSWORD`. En AWS deben venir de Secrets Manager.
- **Frontend:** faltan pruebas (Vitest + Testing Library) y manejo de sesión.

## 5. Cómo correrlo

1. Crear la base de datos en el PostgreSQL local: `CREATE DATABASE proteccion_reto;` (Flyway crea las tablas al arrancar).
2. Copiar `backend/.env.example` como `backend/.env` y poner la URL, el usuario y la clave. Spring lo carga con
   `spring.config.import=optional:file:.env[.properties]`; las variables de entorno con el mismo nombre tienen prioridad.
3. Levantar:

```bash
cd reto-b/backend && ./mvnw spring-boot:run          # http://localhost:8080
cd reto-b/frontend && npm install && npm run dev     # http://localhost:5173
cd reto-b/backend && ./mvnw test                     # las pruebas de integración usan Testcontainers (Docker); sin Docker se omiten
```

## 6. Despliegue en AWS (us-east-2)

```
Navegador ──HTTPS──> CloudFront ──┬── /*      → S3 privado (frontend; solo legible por la distribución vía OAC)
                                  └── /api/*  → EC2 t3.small :8080 (Spring Boot + PostgreSQL 15 en Docker)
```

- **Un solo dominio** sirve el frontend y la API: no hace falta CORS, y la ruta relativa `/api` del frontend funciona igual que con el proxy de Vite.
- **El bucket S3 está bloqueado al público.** Solo lo lee esta distribución, mediante Origin Access Control y una condición `AWS:SourceArn`.
- **El security group de la EC2 solo admite el puerto 8080** desde la lista de prefijos administrada de CloudFront (`com.amazonaws.global.cloudfront.origin-facing`). No tiene SSH: la administración es por **SSM Session Manager**.
- **PostgreSQL solo escucha en `127.0.0.1`.** Su contraseña se genera en la propia instancia (`/etc/reto-b/env`, solo legible por root) y nunca sale de ella.
- **IMDSv2 obligatorio**, disco cifrado, y el rol IAM tiene permisos mínimos (SSM + `s3:GetObject` del jar).
- **Caché:** los assets con hash se cachean un año; `index.html` usa `no-cache`; `/api/*` no se cachea.

Scripts en `infra/` (Git Bash + AWS CLI):

| Script | Qué hace |
|---|---|
| `deploy.sh` | Crea todo. Es reanudable: los ids quedan en `infra/estado.env` |
| `actualizar-backend.sh` | Compila y prueba, sube el jar y reinicia el servicio vía SSM |
| `actualizar-frontend.sh` | Compila, sincroniza S3 e invalida `index.html` |
| `abrir-acceso-publico.sh` | Abre 5432 y 22 (por defecto a internet), instala la llave SSH y expone PostgreSQL |
| `destroy.sh` | Borra todos los recursos (incluida la base de datos de la EC2) |

**Acceso directo para la demo (`infra/abrir-acceso-publico.sh`):** a pedido del candidato, para conectarse desde
DataGrip y por SSH, se abrieron los puertos **5432 (PostgreSQL) y 22 (SSH) a `0.0.0.0/0`**. También se instaló la llave
pública del key pair `access` en `ec2-user` (AWS no permite asignar un key pair a una instancia ya creada) y el
contenedor de PostgreSQL se recreó para escuchar en todas las interfaces, conservando el volumen de datos.
Mitigaciones que se mantienen: SSH sin contraseña (solo llave) y PostgreSQL con `scram-sha-256` y una contraseña aleatoria
de 48 caracteres. **Es una concesión de la demo, no algo aceptable en producción.** Se debería limitar al menos a una IP
(`CIDR=x.x.x.x/32 bash abrir-acceso-publico.sh`) o volver al túnel de SSM, y cerrar los puertos al terminar.

**Lo que falta para producción:**
- El tramo CloudFront → EC2 va por HTTP. Haría falta un ALB con certificado ACM o TLS en el origen.
- PostgreSQL en la misma instancia no tiene alta disponibilidad ni backups. Lo correcto es RDS en subred privada.
- Si la instancia se detiene y se vuelve a iniciar, cambia su DNS público y hay que actualizar el origen. Una IP elástica o un ALB lo evitan.
- No hay autenticación ni WAF delante de CloudFront.
- Todo se creó con la AWS CLI. En el CIS iría como IaC (Terraform/CloudFormation) en un pipeline de GitLab CI.
