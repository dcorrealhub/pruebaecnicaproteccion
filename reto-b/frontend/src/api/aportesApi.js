const BASE_URL = '/api/aportes'

export async function registrarAporte(data) {
  const res = await fetch(BASE_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  })
  if (!res.ok) {
    const msg = await res.text().catch(() => 'Error al registrar aporte')
    throw new Error(msg)
  }
  return res.json()
}

export async function consultarConsolidado({ afiliadoId, periodoDesde, periodoHasta }) {
  const params = new URLSearchParams({ afiliadoId, periodoDesde, periodoHasta })
  const res = await fetch(`${BASE_URL}/consolidado?${params}`)
  if (!res.ok) {
    const msg = await res.text().catch(() => 'Error al consultar consolidado')
    throw new Error(msg)
  }
  return res.json()
}
