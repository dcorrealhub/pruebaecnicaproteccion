# Reto B — Registro y consulta de aportes voluntarios

## Descripción

Implementación de una funcionalidad acotada sobre el stack Spring Boot + PostgreSQL + React, correspondiente al reto técnico de construcción asistida.

La funcionalidad cubre el registro y consulta de aportes voluntarios de afiliados a un fondo, incluyendo reglas de negocio, persistencia correcta y una vista React funcional.

---

## Decisiones de ingeniería

### Arquitectura

Se adoptó **arquitectura hexagonal (ports & adapters)**, que ya venía esbozada en la estructura base del proyecto. Se respetó estrictamente esa separación:

- `domain` — modelos y puertos, sin dependencias de framework
- `application` — casos de uso que orquestan la lógica de negocio
- `infrastructure` — adaptadores JPA y controladores REST

Esta separación permite testear la lógica de negocio de forma aislada, sin necesidad de levantar base de datos ni contexto Spring.

### Dominio

Las clases de dominio (`Aporte`, `SaldoMensual`, `ConsolidadoAportes`) son **puras de Java**, inmutables donde corresponde y sin ninguna anotación de framework. `SaldoMensual` expone métodos como `calcularNuevoTotal()` y `conTotal()` para operar sin mutar estado.

### Reglas de negocio (en `RegistrarAporteUseCaseImpl`)

| Regla | Implementación |
|-------|---------------|
| Monto positivo | Validación explícita con `IllegalArgumentException` y mensaje claro |
| Idempotencia | Búsqueda por `idempotenciaKey` antes de persistir; si existe, se retorna el aporte existente |
| Tope mensual | Se consulta el `SaldoMensual` acumulado del afiliado; se rechaza si el nuevo total lo supera |
| Umbral de revisión | Si el monto supera el umbral configurado, el aporte se persiste con `marcadaRevision = true` |

Los parámetros `tope-mensual` y `umbral-revision` son configurables vía `application.properties`, sin hardcodear valores en el código.

### Concurrencia

`SaldoMensual` usa control de concurrencia **optimista** a través de `@Version` en la entidad JPA. Si dos transacciones intentan actualizar el mismo saldo simultáneamente, JPA lanza `OptimisticLockException` automáticamente.

### Persistencia

Flyway gestiona el esquema. El archivo `V1__init.sql` define las tablas `aporte`, `saldo_mensual` y `evento_aporte`, con los índices necesarios para las consultas por afiliado y periodo.

### Frontend

Vista React funcional con dos componentes:

- `RegistrarAporte` — formulario con validación de monto, selector de canal y generación automática de `idempotenciaKey` vía `crypto.randomUUID()`
- `ConsolidadoAportes` — filtros por afiliado y rango de periodo, tabla con detalle e indicador visual de aportes marcados para revisión

El proxy de Vite redirige `/api` al backend en `localhost:8082`, evitando problemas de CORS en desarrollo.

---

## Problemas encontrados y soluciones

**Nombre de métodos en los puertos de salida**
Al implementar los adaptadores JPA, se usó inicialmente `save()` como nombre del método de persistencia. Al revisar los puertos definidos (`AporteRepositoryPort`, `SaldoRepositoryPort`), el método correcto era `guardar()`. Se corrigió en el adaptador y en el caso de uso.

**Compatibilidad de Java con `.toList()`**
Al mapear listas en `JpaAporteRepositoryAdapter`, se usó `.toList()` directamente sobre el stream, que requiere Java 16+. Al no estar garantizada esa versión en el proyecto, se reemplazó por `.collect(Collectors.toList())` para mayor compatibilidad.

**Inicialización del saldo mensual**
Para afiliados sin saldo previo en el mes, el puerto `SaldoRepositoryPort` expone un método `inicializar()` dedicado, en lugar de construir el objeto directamente en el caso de uso. Esto mantiene la responsabilidad de creación en la capa de infraestructura.

---

## Tests

Se priorizaron tests de unidad sobre la lógica de negocio más crítica (`RegistrarAporteUseCaseImpl`), usando Mockito para aislar dependencias:

- Flujo feliz de registro
- Idempotencia — reintento con misma clave retorna el aporte existente sin duplicar
- Rechazo de monto negativo o cero
- Rechazo por superación del tope mensual
- Marcado para revisión al superar el umbral

Se conservó el test de integración `RetoBApplicationTest` como smoke test que verifica que el contexto de Spring levanta correctamente.

---

## Cómo ejecutar

```bash
# Base de datos (PostgreSQL local)
psql -U postgres -c "CREATE DATABASE proteccion_reto;"

# Backend
./mvnw spring-boot:run

# Frontend
cd frontend
npm install
npm run dev
```

**Endpoints:**
```
POST http://localhost:8082/api/aportes
GET  http://localhost:8082/api/aportes/consolidado?afiliadoId=AF-001&periodoDesde=2025-01&periodoHasta=2025-06
```

**UI:** `http://localhost:5173`

---

## Herramientas de apoyo

Durante el desarrollo se usó IA (Claude y Gemini) como herramienta de asistencia para implementar los métodos marcados como `TODO` en la estructura base del proyecto y indagar sobre conceptos nuevos. El proceso fue iterativo: se compartía cada clase con su contenido existente, se revisaba lo que ya estaba definido, y se completaba únicamente lo que faltaba, respetando en todo momento las convenciones, nombres y decisiones de diseño ya establecidas en el proyecto base.