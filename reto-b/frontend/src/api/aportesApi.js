const BASE_URL = '/api/aportes'

/**
 * Registra un aporte voluntario.
 * @param {{ afiliadoId: string, monto: number, canal: string, idempotenciaKey: string }} data
 * @returns {Promise<object>} aporte creado
 */
export async function registrarAporte(data) {
  const response = await fetch(BASE_URL, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(data),
  })

  if (!response.ok) {
    throw new Error(`registrarAporte: error ${response.status}`)
  }

  return response.json()
}

/**
 * Consulta el consolidado de aportes de un afiliado en un periodo.
 * @param {{ afiliadoId: string, periodoDesde: string, periodoHasta: string }} params
 * @returns {Promise<object>} consolidado con total y detalle
 */
export async function consultarConsolidado({ afiliadoId, periodoDesde, periodoHasta }) {
  const params = new URLSearchParams({ afiliadoId, periodoDesde, periodoHasta })
  const response = await fetch(`${BASE_URL}/consolidado?${params}`, {
    method: 'GET',
  })

  if (!response.ok) {
    throw new Error(`consultarConsolidado: error ${response.status}`)
  }

  return response.json()
}
