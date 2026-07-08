# Reto B · Registro y consulta de aportes voluntarios

Backend Spring Boot 3 (`reto-b/backend`) + frontend React (`reto-b/frontend`).
No usa Lombok. Ajustado para compilar en entornos con Java 17/21 según corresponda.

## Prerrequisitos
- Java 17/21
- Maven
- Node.js 18+
- PostgreSQL 14+
- Base de datos `reto_b` creada en PostgreSQL

## Configuración
- Backend: `reto-b/backend/src/main/resources/application.yml`
  - Datasource: `jdbc:postgresql://localhost:5432/reto_b`
  - Usuario: `postgres`
  - Password: `postgres`
- Propiedades de negocio: `aporte.tope-mensual` y `aporte.umbral-revision`

## Ejecución
1. Backend:
   - desde `reto-b/backend` ejecutar `mvn spring-boot:run`
2. Frontend:
   - desde `reto-b/frontend` ejecutar `npm run dev`

## Prueba rápida
- `GET http://localhost:8080/api/aportes/consolidado?idAfiliado=AF-001&periodoDesde=2025-06&periodoHasta=2025-07`
- `POST http://localhost:8080/api/aportes`
  - body JSON con `idAfiliado`, `monto`, `canal`, `idempotenciaKey`