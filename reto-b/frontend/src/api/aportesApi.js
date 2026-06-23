const BASE_URL = '/api/aportes'

/**
 * Registra un aporte voluntario.
 *
 * @param {{ afiliadoId: string, monto: number, canal: string, idempotenciaKey: string }} data
 * @returns {Promise<object>} aporte creado { id, afiliadoId, monto, fecha, canal, periodo, marcadaRevision }
 * @throws {Error} si el servidor responde con un status de error
 */
export async function registrarAporte(data) {
  const response = await fetch(BASE_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  })

  if (!response.ok) {
    // Intentar extraer el mensaje de error del cuerpo; si falla, usar el status
    const mensaje = await response.text().catch(() => response.statusText)
    throw new Error(mensaje || `Error ${response.status}`)
  }

  return response.json()
}

/**
 * Consulta el consolidado de aportes de un afiliado en un rango de periodos.
 *
 * @param {{ afiliadoId: string, periodoDesde: string, periodoHasta: string }} params
 * @returns {Promise<object>} consolidado { afiliadoId, periodoDesde, periodoHasta, totalAportado, detalle[] }
 * @throws {Error} si el servidor responde con un status de error
 */
export async function consultarConsolidado({ afiliadoId, periodoDesde, periodoHasta }) {
  const params = new URLSearchParams({ afiliadoId, periodoDesde, periodoHasta })
  const response = await fetch(`${BASE_URL}/consolidado?${params}`)

  if (!response.ok) {
    const mensaje = await response.text().catch(() => response.statusText)
    throw new Error(mensaje || `Error ${response.status}`)
  }

  return response.json()
}
