# Reto B — Aportes Voluntarios

## Requisitos previos

```bash
# PostgreSQL
docker compose up -d   # desde la raíz del proyecto
```

## Backend (puerto 8082)

### Registrar un aporte

```bash
curl -s -X POST http://localhost:8082/api/aportes \
  -H "Content-Type: application/json" \
  -d '{"afiliadoId":"AF-001","monto":500000,"canal":"APP_MOVIL","idempotenciaKey":"key-unica-1"}'
```

### Idempotencia (mismo key, mismo resultado)

```bash
curl -s -X POST http://localhost:8082/api/aportes \
  -H "Content-Type: application/json" \
  -d '{"afiliadoId":"AF-001","monto":500000,"canal":"APP_MOVIL","idempotenciaKey":"key-unica-1"}'
```

### Consultar consolidado

```bash
curl -s "http://localhost:8082/api/aportes/consolidado?afiliadoId=AF-001&periodoDesde=2026-01&periodoHasta=2026-06"
```

### Consultar consolidado — periodos invertidos

```bash
curl -s "http://localhost:8082/api/aportes/consolidado?afiliadoId=AF-001&periodoDesde=2026-06&periodoHasta=2026-01"
```

### Consultar consolidado — sin resultados

```bash
curl -s "http://localhost:8082/api/aportes/consolidado?afiliadoId=AF-999&periodoDesde=2026-01&periodoHasta=2026-06"
```

### Errores de validación

```bash
# Monto inválido (negativo)
curl -s -X POST http://localhost:8082/api/aportes \
  -H "Content-Type: application/json" \
  -d '{"afiliadoId":"AF-001","monto":-100,"canal":"APP_MOVIL","idempotenciaKey":"key-error-1"}'

# Tope mensual excedido
curl -s -X POST http://localhost:8082/api/aportes \
  -H "Content-Type: application/json" \
  -d '{"afiliadoId":"AF-001","monto":999999999,"canal":"APP_MOVIL","idempotenciaKey":"key-error-2"}'

# Campos obligatorios vacíos
curl -s -X POST http://localhost:8082/api/aportes \
  -H "Content-Type: application/json" \
  -d '{"afiliadoId":"","monto":500000,"canal":"","idempotenciaKey":""}'
```

## Frontend (puerto 5173)

```bash
cd frontend
npm install
npm run dev
```
