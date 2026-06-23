const API_BASE = import.meta.env.VITE_API_URL || '/api';

async function parseResponse(response) {
  const text = await response.text();
  const data = text ? JSON.parse(text) : null;
  if (!response.ok) {
    const message = data?.mensaje || data?.message || `Error ${response.status}`;
    throw new Error(message);
  }
  return data;
}

export async function registrarAporte(payload, idempotencyKey) {
  const response = await fetch(`${API_BASE}/aportes`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Idempotency-Key': idempotencyKey,
    },
    body: JSON.stringify(payload),
  });
  return parseResponse(response);
}

export async function consultarConsolidado(afiliadoId, periodo) {
  const params = new URLSearchParams({
    afiliadoId,
    periodoDesde: periodo,
    periodoHasta: periodo,
  });
  const response = await fetch(`${API_BASE}/aportes/consolidado?${params}`);
  return parseResponse(response);
}
