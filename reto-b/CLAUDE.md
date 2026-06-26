# CLAUDE.md — Reto B: Aportes Voluntarios

Guia tecnica para el agente de Claude Code. Leer completo antes de tocar cualquier archivo del proyecto.

---

## Contexto del proyecto

API REST en Spring Boot 3.4.1 + PostgreSQL para registrar y consultar aportes voluntarios a un fondo de pensiones, bajo supervision de la Superintendencia Financiera de Colombia (SFC). El frontend es React 18 con Vite.

---

## Reglas absolutas

1. **Correccion numerica:** Usar siempre `BigDecimal` para montos. Nunca `double` ni `float`.
2. **Inmutabilidad del dominio:** Los modelos en `domain/model/` son clases finales con campos finales. No agregar setters.
3. **Idempotencia:** Toda operacion de registro debe ser segura de reintentar. La clave `idempotenciaKey` con restriccion UNIQUE en DB garantiza esto.
4. **Sin SQL directo:** Usar unicamente Spring Data JPA o JPQL. Nunca concatenar strings en queries.
5. **Transacciones:** `RegistrarAporteUseCaseImpl` debe tener `@Transactional`. `ConsultarAportesUseCaseImpl` debe tener `@Transactional(readOnly = true)`.
6. **Excepciones de dominio:** Lanzar `IllegalArgumentException` con mensaje descriptivo desde los use cases. El `GlobalExceptionHandler` hace la traduccion a HTTP.

---

## Rama y commits

- **Rama:** `candidato/tomas-rios` — nunca commitear a `main` o `master`
- **Formato obligatorio:**
  - `feat(reto-b): descripcion en presente` — nueva funcionalidad
  - `fix(reto-b): descripcion en presente` — correccion de bug
- **Antes de cada commit:** verificar que `./mvnw test` pasa sin errores

---

## Arquitectura hexagonal — como esta organizado

```
domain/          Nucleo puro de Java. Sin anotaciones de Spring ni JPA.
  model/         Aporte, SaldoMensual, ConsolidadoAportes
  port/in/       Casos de uso (interfaces): RegistrarAporteUseCase, ConsultarAportesUseCase
  port/out/      Puertos de salida (interfaces): AporteRepositoryPort, SaldoRepositoryPort

application/     Implementaciones de los casos de uso. Logica de negocio aqui.
  usecase/       RegistrarAporteUseCaseImpl, ConsultarAportesUseCaseImpl

infrastructure/  Adaptadores que conectan el mundo exterior con el dominio.
  persistence/   Entidades JPA, Spring Data repos, adaptadores JPA
  web/           Controller, DTOs, GlobalExceptionHandler
```

**Regla de dependencias:** `infrastructure` depende de `application` y `domain`. `domain` no depende de nadie.

---

## Variables de configuracion

En `application.properties`:

```properties
aporte.tope-mensual=10000000    # Tope mensual por afiliado en COP
aporte.umbral-revision=5000000  # Monto a partir del cual se marca REQUIERE_REVISION
```

Inyectar en los use cases con `@Value("${aporte.tope-mensual:10000000}")`.

---

## Flujo de registro de aporte (RegistrarAporteUseCaseImpl)

```
1. Buscar por idempotenciaKey -> si ya existe, retornar el aporte existente (idempotencia)
2. Validar monto > 0 -> IllegalArgumentException si no
3. Cargar SaldoMensual del mes actual (o inicializar si no existe)
4. Validar tope: saldo.total + monto <= topeMensual -> IllegalArgumentException si supera
5. marcadaRevision = monto > umbralRevision
6. Construir Aporte con fecha=hoy, periodo=YYYY-MM
7. aporteRepository.guardar(aporte)
8. saldoRepository.guardar(saldo.conTotal(saldo.calcularNuevoTotal(monto)))
```

---

## Endpoints de la API

| Metodo | Ruta | Descripcion |
|--------|------|-------------|
| POST | `/api/aportes` | Registrar aporte (idempotente) |
| GET | `/api/aportes/consolidado?afiliadoId=&periodoDesde=&periodoHasta=` | Consultar consolidado |

**Formato de periodos:** `YYYY-MM` (ej: `2025-01`)

---

## Errores comunes a evitar

```java
// MAL — usa double para dinero
double monto = 100000.50;

// BIEN — BigDecimal siempre
BigDecimal monto = new BigDecimal("100000.50");
```

```java
// MAL — logica de negocio en el controller
if (req.monto().compareTo(BigDecimal.ZERO) <= 0) return ResponseEntity.badRequest()...

// BIEN — excepcion en el use case, el GlobalExceptionHandler la convierte
throw new IllegalArgumentException("El monto debe ser mayor a cero");
```

---

## Como levantar el proyecto

```bash
# 1. Levantar PostgreSQL
docker compose up -d   # desde reto-b/

# 2. Backend (puerto 8082)
cd backend
./mvnw spring-boot:run

# 3. Frontend (puerto 5173)
cd frontend
npm install
npm run dev
```

El proxy de Vite redirige `/api/*` a `http://localhost:8082`. No modificar esta configuracion.

---

## Suite de tests

### Estructura

```
src/test/
├── resources/
│   ├── application-test.properties          ← perfil H2, Flyway off, springdoc off
│   └── mockito-extensions/
│       └── org.mockito.plugins.MockMaker    ← mock maker subclass (sin inline agent)
└── java/co/proteccion/cis/retob/
    ├── support/AporteMother.java             ← fabrica de objetos de prueba
    ├── domain/model/SaldoMensualTest.java    ← JUnit 5 puro, sin Spring
    ├── application/usecase/
    │   ├── RegistrarAporteUseCaseImplTest.java
    │   └── ConsultarAportesUseCaseImplTest.java
    └── infrastructure/web/AporteControllerTest.java
```

### Convenciones por nivel

| Nivel | Herramienta | Que testea |
|-------|-------------|------------|
| Dominio | JUnit 5 puro | Inmutabilidad y calculo en SaldoMensual |
| Aplicacion | `@ExtendWith(MockitoExtension.class)` | Reglas de negocio en use cases |
| Web | `@WebMvcTest` + `@MockitoBean` | Contratos HTTP, validaciones, excepciones |
| Contexto completo | `@SpringBootTest` + `@ActiveProfiles("test")` | Arranque del contexto con H2 |

### Notas de implementacion

- `@Value` fields (`topeMensual`, `umbralRevision`) no son `final`, por lo que `@InjectMocks` no los inyecta. Usar `ReflectionTestUtils.setField()` en `@BeforeEach`.
- `ConsolidadoAportes` es un `record` — usar sintaxis de componente (`.totalAportado()`, no `.getTotalAportado()`).
- `maven-surefire-plugin` con `-XX:+EnableDynamicAgentLoading -Xshare:off --enable-native-access=ALL-UNNAMED` suprime warnings de JVM en ejecucion.
- `springdoc.api-docs.enabled=false` en el perfil test evita que la autoconfiguracion de springdoc interfiera con `@WebMvcTest`.
- `@MockitoBean` (Spring Boot 3.4+) reemplaza el `@MockBean` deprecado.

### Antes de cada commit

```bash
cd backend && ./mvnw test   # 32 tests, 0 failures, BUILD SUCCESS
```
