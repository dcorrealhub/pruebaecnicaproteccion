# Prueba Técnica AI-First · CIS Protección S.A.

Repositorio base para la prueba técnica de nivel senior del Centro de Ingeniería de Software (CIS).

## Estructura del repositorio

```
pruebaecnicaproteccion/
├── reto-a/          # Módulo a auditar — Spring Boot con defectos deliberados
└── reto-b/
    ├── backend/     # Scaffold Spring Boot (Clean Architecture) para implementar
    └── frontend/    # Scaffold React + Vite para implementar
```

## Requisitos previos

- Java 21
- Maven 3.9+
- Node 20+
- Docker y Docker Compose (para Reto B)

## Reto A — Auditoría de código

El módulo `reto-a` es un servicio de registro de aportes a un fondo voluntario.
**Compila y los tests felices pasan.** Tu trabajo es revisarlo como si fuera un MR de tu célula.

```bash
cd reto-a
./mvnw test        # Verifica que los tests pasan
./mvnw spring-boot:run  # Levanta el servicio (puerto 8080, H2 en memoria)
```

Endpoints disponibles para exploración manual:

```
POST http://localhost:8080/api/aportes
GET  http://localhost:8080/api/aportes/consolidado?afiliadoId=AF-001&periodo=2025-06
```

Payload de ejemplo para el POST:
```json
{
  "afiliadoId": "AF-001",
  "monto": 500000.0,
  "canal": "APP_MOVIL"
}
```

## Reto B — Construcción asistida

### Base de datos (PostgreSQL vía Docker)

```bash
docker compose up -d
```

Esto levanta PostgreSQL en `localhost:5432` con base de datos `proteccion_reto`, usuario `postgres`, contraseña `postgres`.

### Backend

```bash
cd reto-b/backend
./mvnw spring-boot:run   # Puerto 8082
```

### Frontend

```bash
cd reto-b/frontend
npm install
npm run dev              # Puerto 5173
```

### API (Reto B)

```
POST http://localhost:8082/api/aportes                      # registrar (idempotente)
GET  http://localhost:8082/api/aportes/consolidado?afiliadoId=AF-001&periodoDesde=2025-06&periodoHasta=2025-06
POST http://localhost:8082/api/aportes/{id}/aprobar         # aprobar un aporte en revisión
POST http://localhost:8082/api/aportes/{id}/rechazar        # rechazar un aporte en revisión
GET  http://localhost:8082/api/configuracion/parametros     # consultar tope y umbral globales
PUT  http://localhost:8082/api/configuracion/parametros     # actualizar tope y umbral (runtime)
```

Payload de registro (`fecha` es opcional; por defecto, hoy):
```json
{
  "afiliadoId": "AF-001",
  "monto": 500000.00,
  "fecha": "2025-06-10",
  "canal": "APP_MOVIL",
  "idempotenciaKey": "f8c3de3d-1fea-4d7c-a8b0-29f63c4c3454"
}
```

**Modelo de estados:** un aporte es `APROBADO`, `PENDIENTE_REVISION` (superó el umbral
configurable) o `RECHAZADO`. Tanto aprobados como pendientes **reservan cupo** del tope
mensual, de modo que un pendiente nunca supere el tope al aprobarse; rechazar libera la
reserva. El tope y el umbral se configuran en la tabla `parametro_aporte`.
Reglas, decisiones y cobertura de pruebas: ver [reto-b/NOTAS_PROCESO.md](reto-b/NOTAS_PROCESO.md).

## Instrucciones para candidatos

1. **No hagas fork.** Clona este repositorio directamente.
2. Crea una rama con tu nombre en formato `candidato/nombre-apellido`:
   ```bash
   git checkout -b candidato/maria-garcia
   ```
3. **Reto A:** Documenta tus hallazgos en `reto-a/HALLAZGOS.md`. No modifiques el código del módulo a auditar.
4. **Reto B:** Implementa la funcionalidad en `reto-b/`. Puedes y debes apoyarte en IA.
5. Conserva tus prompts y notas en `reto-b/NOTAS_PROCESO.md`. Son parte de la entrega.
6. Sube tu rama al repositorio remoto cuando termines:
   ```bash
   git push origin candidato/maria-garcia
   ```

## Stack de referencia CIS

- **Backend:** Spring Boot 3.4.x · Java 21 · Spring Data JPA · PostgreSQL
- **Frontend:** React 18 · Vite · fetch nativo
- **Arquitectura:** Clean Architecture · SOLID · CQRS
- **Seguridad:** OWASP Top 10 · manejo correcto de dinero (BigDecimal) · idempotencia

> Usa datos sintéticos en todo momento. Nunca uses ni inventes información personal real.
