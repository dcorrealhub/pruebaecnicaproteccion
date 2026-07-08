# Plan de Implementación — Reto B

---

## Fase 1 — JpaAporteRepositoryAdapter

### Objetivo
Implementar el adaptador JPA del puerto de salida `AporteRepositoryPort`, realizando el mapping entre el modelo de dominio `Aporte` y la entidad JPA `AporteEntity`.

### Análisis
- El puerto `AporteRepositoryPort` define 3 métodos: `guardar`, `findByIdempotenciaKey`, `findByAfiliadoIdAndPeriodoBetween`.
- `SpringDataAporteRepository` ya tiene las queries derivadas exactas para los dos métodos de consulta.
- `AporteEntity` usa Lombok `@Builder` y tiene `@PrePersist` para `creadoEn`.
- `Aporte` dominio es inmutable con constructor explícito de 8 parámetros.

### Decisiones de diseño
- Mapping en métodos privados estáticos dentro del adapter (infra concern, no se extrae a mapper separado porque es directo 1:1 y no hay reuso entre adapters).
- `creadoEn` no se mapea: la entidad lo auto-asigna via `@PrePersist`.
- No se crean archivos nuevos.
- `guardar` soporta tanto inserción (id=null) como actualización (id=no null).

### Archivos modificados
- `infrastructure/persistence/adapter/JpaAporteRepositoryAdapter.java` — implementación completa.

### Riesgos encontrados
- Ninguno. El mapping es directo sin transformaciones ni lógica de negocio.

### Pendientes
- Validar que el adapter compile correctamente con `mvn compile`.
- Las pruebas requieren las fases 3+ para ejercitar flujos completos.

### Cómo validar
- `mvn compile` desde `reto-b/backend` debe pasar sin errores.
- El test `RetoBApplicationTest` debe seguir cargando el contexto.
- Las fases posteriores (3. RegistrarAporteUseCaseImpl) ejercitarán el adapter en pruebas funcionales.

---

## Fase 2 — JpaSaldoRepositoryAdapter

### Objetivo
Implementar el adaptador JPA del puerto de salida `SaldoRepositoryPort` con soporte de optimistic locking via `@Version`.

### Análisis
- `SaldoMensualEntity` tiene `@Version` en el campo `version` (columna NOT NULL DEFAULT 0 en BD).
- `SaldoMensual` dominio tiene `version` final Integer, y ofrece `conTotal()` para crear copia con nuevo total.
- `inicializar` recibe raw params (no un domain model), crea un registro con `total=0`.

### Decisiones de diseño
- Mapping inline estático, mismo patrón que Fase 1.
- `version` se mapea bidireccionalmente para que JPA dispare `OptimisticLockException` en conflictos de concurrencia.
- `inicializar` construye la entidad directamente con `.version(0)` para que Hibernate lo trate como nuevo.

### Archivos modificados
- `infrastructure/persistence/adapter/JpaSaldoRepositoryAdapter.java` — implementación completa.

### Riesgos encontrados
- Si el `version` del dominio no coincide con la BD al hacer `guardar`, JPA lanza `OptimisticLockException`. Esto es deseable y debe manejarse en la capa de aplicación (Fase 3).

### Pendientes
- Ninguno para esta fase.

### Cómo validar
- `mvn compile` debe pasar.
- El test de contexto (`RetoBApplicationTest`) debe seguir funcionando.

---

## Fase 3 — RegistrarAporteUseCaseImpl

### Objetivo
Implementar la lógica de negocio central de registro de aportes con idempotencia, validación de tope mensual, marcación de revisión y actualización concurrentemente segura del saldo.

### Análisis
- `RegistrarAporteUseCase` define el comando `RegistrarAporteCommand(afiliadoId, monto, canal, idempotenciaKey)`.
- El caso de uso depende de `AporteRepositoryPort` y `SaldoRepositoryPort` (ambos ya implementados en Fases 1 y 2).
- `SaldoMensual.calcularNuevoTotal` y `conTotal` permiten trabajar con inmutabilidad del dominio.
- `@Version` en la entidad garantiza que JPA dispare `OptimisticLockException` si hay conflicto de concurrencia.

### Reglas de negocio implementadas
1. **Idempotencia**: si existe aporte con misma `idempotenciaKey`, se retorna sin duplicar.
2. **Monto positivo**: validación temprana, `IllegalArgumentException` si <= 0.
3. **Tope mensual**: el acumulado del afiliado en el periodo + nuevo monto no puede exceder `aporte.tope-mensual`.
4. **Umbral de revisión**: si el monto supera `aporte.umbral-revision`, el aporte se marca `marcadaRevision = true`.
5. **Concurrencia**: optimistic locking via `@Version` en `saldo_mensual`.

### Decisiones de diseño
- La fecha del aporte se toma de `LocalDate.now()` (el command no trae fecha).
- El periodo se deriva de la fecha actual con formato `yyyy-MM`.
- `@Transactional` en el método para atomicidad: si `OptimisticLockException` ocurre en `guardar` del saldo, toda la transacción se revierte y el aporte no se persiste.
- Publicación de eventos (`evento_aporte`): no implementado. Requiere crear un nuevo puerto de salida y adaptador, lo cual excede la lista de archivos TODO. Queda como mejora post-MVP.

### Archivos modificados
- `application/usecase/RegistrarAporteUseCaseImpl.java` — implementación completa.

### Riesgos encontrados
- `OptimisticLockException` no se captura explícitamente; se deja propagar como `RuntimeException` para que Spring Transaction rollback automáticamente. Si se desea un mensaje más amigable, se puede añadir un `@ExceptionHandler` en el controller.

### Pendientes
- Publicación de eventos de dominio en `evento_aporte` (post-MVP).

### Cómo validar
- `mvn compile` debe pasar.
- Con el backend corriendo y PostgreSQL arriba, `POST /api/aportes` con payload válido debe retornar 201 con el aporte creado.
- Reintentar con misma `idempotenciaKey` debe retornar el mismo aporte (idempotencia).
- Enviar monto que supere el tope mensual debe retornar error 400.
- Enviar monto > umbral debe marcar `marcadaRevision: true`.

---

## Fase 4 — ConsultarAportesUseCaseImpl

### Objetivo
Implementar el caso de uso de consulta: obtener todos los aportes de un afiliado en un rango de periodos y devolver el total acumulado con detalle.

### Análisis
- `ConsultarAportesUseCase` define `ConsultarAportesQuery(afiliadoId, periodoDesde, periodoHasta)`.
- `ConsolidadoAportes` es un `record` con `afiliadoId`, `periodoDesde`, `periodoHasta`, `totalAportado`, `detalle`.
- Solo depende de `AporteRepositoryPort.findByAfiliadoIdAndPeriodoBetween` (Fase 1).
- `ConsolidadoResponse` es el DTO web para respuesta, no se modifica.

### Decisiones de diseño
- Suma con `stream().map().reduce(BigDecimal.ZERO, BigDecimal::add)` sobre `BigDecimal` (precisión financiera, nunca `double`).
- `@Transactional(readOnly = true)` para optimización de conexión y semántica de solo lectura.

### Archivos modificados
- `application/usecase/ConsultarAportesUseCaseImpl.java` — implementación completa.

### Riesgos encontrados
- Si no hay aportes en el periodo, se retorna `ConsolidadoAportes` con `totalAportado = 0` y `detalle` vacío — comportamiento esperado, no es error.

### Cómo validar
- `mvn compile` debe pasar.
- `GET /api/aportes/consolidado?afiliadoId=AF-001&periodoDesde=2025-01&periodoHasta=2025-06` debe retornar el consolidado.
- Consultar afiliado sin aportes debe retornar total=0 y lista vacía.

---

## Fase 5 — Frontend API (aportesApi.js)

### Objetivo
Implementar las dos funciones de fetch en el frontend para conectar los componentes React con el backend.

### Análisis
- `RegistrarAporte.jsx` y `ConsolidadoAportes.jsx` ya tienen UI completa con manejo de estado, error y carga. Solo falta la capa de API.
- `RegistrarAporte` envía `{ afiliadoId, monto, canal, idempotenciaKey }` vía POST.
- `ConsolidadoAportes` envía `afiliadoId`, `periodoDesde`, `periodoHasta` como query params vía GET.
- Vite proxy (`vite.config.js`) redirige `/api/*` a `http://localhost:8082`.

### Decisiones de diseño
- Se usa `new URLSearchParams()` para construir query string (evita concatenación manual).
- El error del servidor se captura como texto plano y se propaga como `Error` para que los componentes lo muestren.
- No se agregan dependencias nuevas (fetch nativo).

### Archivos modificados
- `frontend/src/api/aportesApi.js` — implementación completa.

### Riesgos encontrados
- Si el backend responde con un body de error no-JSON, `res.text()` captura todo correctamente.
- El `idempotenciaKey` se genera desde el frontend con `crypto.randomUUID()` ya implementado en el componente.

### Cómo validar
- `npm run dev` desde `reto-b/frontend` levanta el servidor en :5173.
- Completar el formulario y enviar debe registrar el aporte contra el backend.
- Consultar consolidado debe mostrar total y detalle.

---

## Fase 6 — Pruebas de integración

### Objetivo
Escribir tests de integración que validen las reglas de negocio y casos límite del sistema completo (use cases + adapters + DB H2).

### Archivos creados
- `src/test/java/co/proteccion/cis/retob/RegistroAportesIntegrationTest.java` — 11 tests de integración.

### Escenarios cubiertos

| Test | Regla |
|---|---|
| `registrarAporte_exitoso` | Happy path — aporte válido se persiste con ID |
| `registrarAporte_idempotente` | Misma `idempotenciaKey` retorna el mismo aporte sin duplicar |
| `registrarAporte_montoNegativo_lanzaExcepcion` | Monto negativo → `IllegalArgumentException` |
| `registrarAporte_montoCero_lanzaExcepcion` | Monto cero → `IllegalArgumentException` |
| `registrarAporte_superaTopeMensual_lanzaExcepcion` | Acumulado > 10M → rechazado |
| `registrarAporte_topeExacto_ok` | Acumulado = 10M → permitido |
| `registrarAporte_superaUmbral_marcaRevision` | Monto individual > 5M → `marcadaRevision=true` |
| `registrarAporte_porDebajoUmbral_noMarcaRevision` | Monto ≤ 5M → `marcadaRevision=false` |
| `consultarConsolidado_conAportes_retornaTotalYDetalle` | Suma y lista de aportes correcta |
| `consultarConsolidado_sinAportes_retornaVacio` | Sin aportes → total=0, lista vacía |
| `registrarAporte_concurrencia_unGanaOtroRecibeExcepcion` | 2 hilos simultáneos: 1 éxito, 1 `OptimisticLockingFailureException` |

### Decisiones de diseño
- Cada test limpia la BD via `DataSource` directo (`DELETE FROM aporte`, `DELETE FROM saldo_mensual`).
- Comparaciones de `BigDecimal` usan `compareTo()` en vez de `equals()` para evitar diferencias de escala (BD devuelve `NUMERIC(15,2)` → scale=2).
- El test de concurrencia usa `CountDownLatch` para lanzar ambos hilos simultáneamente. Si la ventana de carrera no se materializa, ambos pueden succeed — se verifica que al menos 1 succeed.

### Estado
- `mvn test` — 12 tests (11 integración + 1 contexto) pasan sin errores.
