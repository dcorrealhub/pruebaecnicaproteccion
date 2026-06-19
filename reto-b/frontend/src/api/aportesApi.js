const BASE_URL = '/api/aportes'

async function parseResponse(res) {
  const body = await res.json().catch(() => ({}))
  if (!res.ok) {
    const msg = body.detail || body.message || `Error ${res.status}`
    throw new Error(msg)
  }
  return body
}

export async function registrarAporte(data) {
  const res = await fetch(BASE_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  })
  return parseResponse(res)
}

export async function consultarConsolidado({ afiliadoId, periodoDesde, periodoHasta }) {
  const params = new URLSearchParams({ afiliadoId, periodoDesde, periodoHasta })
  const res = await fetch(`${BASE_URL}/consolidado?${params}`)
  return parseResponse(res)
}
