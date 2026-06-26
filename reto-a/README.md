# Reto A — Módulo de aportes voluntarios

## Endpoints

### Registrar un aporte

```bash
curl -s -X POST http://localhost:8080/api/aportes \
  -H "Content-Type: application/json" \
  -d '{"afiliadoId":"AF-001","monto":500000.0,"canal":"APP_MOVIL"}'
```

### Consultar consolidado

```bash
curl -s "http://localhost:8080/api/aportes/consolidado?afiliadoId=AF-001&periodo=2026-06"
```
