# Frontend — Aportes voluntarios

React + Vite. Consume el backend en `localhost:8082` vía proxy de desarrollo.

## Requisitos

- Node.js 18+

## Arranque

```bash
cd reto-b/frontend
npm install
npm run dev
```

Abre http://localhost:5173

El proxy de Vite reenvía `/api` → `http://localhost:8082`.

## Endpoints usados

- `POST /api/aportes` (header `Idempotency-Key`)
- `GET /api/aportes/consolidado?afiliadoId=&periodoDesde=&periodoHasta=` (mismo periodo en ambos)

## Variables de entorno

Opcional: `VITE_API_URL=http://localhost:8082/api` si no usas el proxy.
