const BASE_URL = '/api/aportes'
const MAX_REINTENTOS_CONCURRENCIA = 2

/** Error de la API con el código estable que envía el backend (ProblemDetail.codigo). */
export class ApiError extends Error {
  constructor(mensaje, { status, codigo, errores } = {}) {
    super(mensaje)
    this.status = status
    this.codigo = codigo
    this.errores = errores ?? []
  }
}

async function leerError(res) {
  let problema = {}
  try {
    problema = await res.json()
  } catch {
    // respuesta sin cuerpo JSON (p. ej. proxy caído)
  }
  const mensaje = problema.detail || `Error ${res.status} al comunicarse con el servidor`
  return new ApiError(mensaje, { status: res.status, codigo: problema.codigo, errores: problema.errores })
}

const esperar = ms => new Promise(r => setTimeout(r, ms))

/**
 * Registra un aporte voluntario.
 *
 * La idempotenciaKey la genera quien llama y debe ser la MISMA para todos los intentos
 * del mismo aporte: así un reintento (manual o automático) nunca lo duplica.
 * El monto se envía como string decimal para no perder precisión con números de punto flotante.
 *
 * @param {{ afiliadoId: string, monto: string, canal: string, idempotenciaKey: string }} data
 * @returns {Promise<{ aporte: object, reintento: boolean }>} reintento=true si el servidor devolvió un aporte ya registrado
 */
export async function registrarAporte(data) {
  for (let intento = 0; ; intento++) {
    const res = await fetch(BASE_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    })

    if (res.ok) {
      return { aporte: await res.json(), reintento: res.status === 200 }
    }

    const error = await leerError(res)
    // Un conflicto de concurrencia es transitorio y, con la misma clave, reintentar es seguro
    if (error.codigo === 'CONFLICTO_CONCURRENCIA' && intento < MAX_REINTENTOS_CONCURRENCIA) {
      await esperar(200 * (intento + 1))
      continue
    }
    throw error
  }
}

/**
 * Consulta el consolidado de aportes de un afiliado en un periodo.
 * @param {{ afiliadoId: string, periodoDesde: string, periodoHasta: string }} params
 * @returns {Promise<object>} consolidado con total y detalle
 */
export async function consultarConsolidado({ afiliadoId, periodoDesde, periodoHasta }) {
  const query = new URLSearchParams({ afiliadoId, periodoDesde, periodoHasta })
  const res = await fetch(`${BASE_URL}/consolidado?${query}`)
  if (!res.ok) {
    throw await leerError(res)
  }
  return res.json()
}
