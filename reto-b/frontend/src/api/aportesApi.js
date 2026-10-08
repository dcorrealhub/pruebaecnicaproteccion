const BASE_URL = '/api/aportes'

/**
 * Convierte una respuesta de error (ProblemDetail del backend) en un Error con mensaje legible.
 * Incluye los errores por campo cuando vienen (validaciones 400).
 */
async function errorDesdeRespuesta(response) {
  let problema = null
  try {
    problema = await response.json()
  } catch {
    // el cuerpo no es JSON (ej. proxy caído): se usa un mensaje genérico
  }

  const detalle = problema?.detail ?? `Error inesperado (HTTP ${response.status})`
  const errores = Object.entries(problema?.errores ?? {}).map(([campo, mensaje]) => `${campo}: ${mensaje}`)

  const error = new Error(errores.length > 0 ? `${detalle} — ${errores.join('; ')}` : detalle)
  error.codigo = problema?.codigo
  error.status = response.status
  return error
}

/**
 * Registra un aporte voluntario.
 * El monto se envía como texto para no perder precisión decimal en la conversión a número.
 * @param {{ afiliadoId: string, monto: string, fecha: string, canal: string, idempotenciaKey: string }} data
 * @returns {Promise<{ aporte: object, repetido: boolean }>} aporte registrado y si fue un reintento (HTTP 200)
 */
export async function registrarAporte(data) {
  const response = await fetch(BASE_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  })
  if (!response.ok) {
    throw await errorDesdeRespuesta(response)
  }
  return { aporte: await response.json(), repetido: response.status === 200 }
}

/**
 * Consulta el consolidado de aportes de un afiliado en un periodo.
 * @param {{ afiliadoId: string, periodoDesde: string, periodoHasta: string }} params
 * @returns {Promise<object>} consolidado con total y detalle
 */
export async function consultarConsolidado({ afiliadoId, periodoDesde, periodoHasta }) {
  const params = new URLSearchParams({ afiliadoId, periodoDesde, periodoHasta })
  const response = await fetch(`${BASE_URL}/consolidado?${params}`)
  if (!response.ok) {
    throw await errorDesdeRespuesta(response)
  }
  return response.json()
}
