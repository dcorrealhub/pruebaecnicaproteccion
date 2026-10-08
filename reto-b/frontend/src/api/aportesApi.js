const BASE_URL = '/api/aportes'

/**
 * Error con el mensaje que devuelve el backend (ErrorResponse: { status, error, mensaje, campos? }).
 */
export class ApiError extends Error {
  constructor(status, mensaje, campos) {
    super(mensaje)
    this.status = status
    this.campos = campos ?? null
  }
}

async function request(url, options) {
  let res
  try {
    res = await fetch(url, options)
  } catch {
    throw new ApiError(0, 'No se pudo conectar con el backend')
  }

  const body = await res.json().catch(() => null)

  if (!res.ok) {
    // Se muestra el mensaje del backend tal cual; solo si no viene se usa el status HTTP
    throw new ApiError(res.status, body?.mensaje ?? `Error HTTP ${res.status}`, body?.campos)
  }
  return { status: res.status, body }
}

/**
 * Registra un aporte voluntario.
 * @param {{ afiliadoId: string, monto: number, canal: string, idempotenciaKey: string }} data
 * @returns {Promise<{ aporte: object, duplicado: boolean }>}
 *   duplicado = true cuando el backend responde 200 (la idempotenciaKey ya existía)
 */
export async function registrarAporte(data) {
  const { status, body } = await request(BASE_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  })
  return { aporte: body, duplicado: status === 200 }
}

/**
 * Consulta el consolidado de aportes de un afiliado en un periodo (yyyy-MM).
 * @param {{ afiliadoId: string, periodo: string }} params
 * @returns {Promise<object>} { afiliadoId, periodoDesde, periodoHasta, totalAportado, detalle[] }
 */
export async function consultarConsolidado({ afiliadoId, periodo }) {
  const qs = new URLSearchParams({ afiliadoId, periodo })
  const { body } = await request(`${BASE_URL}/consolidado?${qs}`)
  return body
}
