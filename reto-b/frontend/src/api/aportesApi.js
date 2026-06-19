const BASE_URL = '/api/aportes'

async function parsearRespuesta(response) {
  const data = await response.json().catch(() => ({}))
  if (!response.ok) {
    const mensaje = data.mensaje || `Error ${response.status}: ${response.statusText}`
    const error = new Error(mensaje)
    error.codigo = data.codigo
    error.status = response.status
    throw error
  }
  return data
}

/**
 * Registra un aporte voluntario.
 * @param {{ afiliadoId: string, monto: number, fecha: string, canal: string }} data
 * @param {string} idempotenciaKey UUID generado por el cliente para garantizar idempotencia
 */
export async function registrarAporte(data, idempotenciaKey) {
  const response = await fetch(BASE_URL, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Idempotency-Key': idempotenciaKey,
    },
    body: JSON.stringify({
      afiliadoId: data.afiliadoId,
      monto: data.monto,
      fecha: data.fecha,
      canal: data.canal,
    }),
  })
  return parsearRespuesta(response)
}

/**
 * Consulta el consolidado de aportes de un afiliado en un periodo.
 * @param {{ afiliadoId: string, periodoDesde: string, periodoHasta: string }} params
 */
export async function consultarConsolidado({ afiliadoId, periodoDesde, periodoHasta }) {
  const params = new URLSearchParams({ periodoDesde, periodoHasta })
  const response = await fetch(`${BASE_URL}/${encodeURIComponent(afiliadoId)}/consolidado?${params}`)
  return parsearRespuesta(response)
}
