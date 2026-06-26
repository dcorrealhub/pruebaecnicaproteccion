const BASE_URL = '/api/aportes'

export async function registrarAporte(data) {
  const response = await fetch(BASE_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  })

  const json = await response.json()

  if (!response.ok) {
    throw new Error(json.mensaje || 'Error al registrar el aporte')
  }

  return json
}

export async function consultarConsolidado({ afiliadoId, periodoDesde, periodoHasta }) {
  const params = new URLSearchParams({ afiliadoId, periodoDesde, periodoHasta })
  const response = await fetch(`${BASE_URL}/consolidado?${params}`)

  const json = await response.json()

  if (!response.ok) {
    throw new Error(json.mensaje || 'Error al consultar el consolidado')
  }

  return json
}
