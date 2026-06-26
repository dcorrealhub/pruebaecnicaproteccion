const BASE_URL = '/api/aportes'

/**
 * Registra un aporte voluntario.
 * @param {{ afiliadoId: string, monto: number, canal: string, idempotenciaKey: string }} data
 * @returns {Promise<object>} aporte creado
 */
export async function registrarAporte(data) {
  const res = await fetch(BASE_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  })
  if (!res.ok) {
    // Lee el campo "mensaje" que devuelve GlobalExceptionHandler; si el cuerpo no es JSON usa el código HTTP.
    const err = await res.json().catch(() => ({}))
    throw new Error(err.mensaje || `Error ${res.status}`)
  }
  return res.json()
}

export async function consultarConsolidado({ afiliadoId, periodoDesde, periodoHasta }) {
  // URLSearchParams serializa los filtros como query string sin necesidad de encodear manualmente.
  const params = new URLSearchParams({ afiliadoId, periodoDesde, periodoHasta })
  const res = await fetch(`${BASE_URL}/consolidado?${params}`)
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.mensaje || `Error ${res.status}`)
  }
  return res.json()
}
