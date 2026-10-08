const BASE_URL = '/api/aportes'

/**
 * Error de API que transporta el ProblemDetail (RFC 7807) devuelto por el backend.
 */
export class ApiError extends Error {
  constructor(message, { status, problem } = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.problem = problem
  }
}

/**
 * Extrae un mensaje legible de una respuesta de error.
 * Prioriza el contrato ProblemDetail (title/detail) y adjunta los errores de campo si vienen.
 */
async function construirError(res) {
  let problem = null
  try {
    problem = await res.json()
  } catch {
    // respuesta sin cuerpo JSON
  }

  let mensaje = problem?.detail || problem?.title || `Error ${res.status}`
  if (Array.isArray(problem?.errores) && problem.errores.length > 0) {
    mensaje += ' (' + problem.errores.join('; ') + ')'
  }
  return new ApiError(mensaje, { status: res.status, problem })
}

/**
 * Registra un aporte voluntario. La operación es idempotente: el cliente genera la
 * idempotenciaKey, de modo que reintentar el mismo envío no duplica el aporte.
 *
 * El monto se envía como string para no perder precisión (dinero no debe viajar como
 * número de punto flotante); el backend lo deserializa a BigDecimal.
 *
 * @param {{ afiliadoId: string, monto: string, fecha: string, canal: string }} data
 * @returns {Promise<object>} aporte creado
 */
export async function registrarAporte({ afiliadoId, monto, fecha, canal }) {
  const res = await fetch(BASE_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      afiliadoId,
      monto: String(monto),
      fecha,
      canal,
      idempotenciaKey: crypto.randomUUID(),
    }),
  })

  if (!res.ok) {
    throw await construirError(res)
  }
  return res.json()
}

/**
 * Consulta el consolidado de aportes de un afiliado en un periodo.
 * @param {{ afiliadoId: string, periodoDesde: string, periodoHasta: string }} params
 * @returns {Promise<object>} consolidado con total y detalle
 */
export async function consultarConsolidado({ afiliadoId, periodoDesde, periodoHasta }) {
  const params = new URLSearchParams({ afiliadoId, periodoDesde, periodoHasta })
  const res = await fetch(`${BASE_URL}/consolidado?${params}`)

  if (!res.ok) {
    throw await construirError(res)
  }
  return res.json()
}
