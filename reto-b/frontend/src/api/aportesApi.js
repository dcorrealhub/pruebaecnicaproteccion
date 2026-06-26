const BASE_URL = '/api/aportes'

async function handleResponse(res) {
  if (res.ok) {
    return res.json()
  }
  const body = await res.json().catch(() => ({}))
  const msg = body.detail || body.title || `Error ${res.status}`
  throw new Error(msg)
}

export async function registrarAporte(data) {
  const res = await fetch(BASE_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  })
  return handleResponse(res)
}

export async function consultarConsolidado({ afiliadoId, periodoDesde, periodoHasta }) {
  const params = new URLSearchParams({ afiliadoId, periodoDesde, periodoHasta })
  const res = await fetch(`${BASE_URL}/consolidado?${params}`)
  return handleResponse(res)
}
