const BASE_URL = '/api/aportes'

/**
 * Extrae un mensaje de error legible del cuerpo de la respuesta.
 * El backend responde con { mensaje, errores } (ver ErrorResponse).
 */
async function lanzarErrorDesde(res) {
  let cuerpo = null
  try {
    cuerpo = await res.json()
  } catch {
    // respuesta sin cuerpo JSON
  }
  if (cuerpo?.errores) {
    const detalle = Object.entries(cuerpo.errores)
      .map(([campo, msg]) => `${campo}: ${msg}`)
      .join(' · ')
    throw new Error(detalle || cuerpo.mensaje || `Error ${res.status}`)
  }
  throw new Error(cuerpo?.mensaje || `Error ${res.status}`)
}

/**
 * Registra un aporte voluntario.
 * @param {{ afiliadoId: string, monto: number, canal: string, idempotenciaKey: string, fecha?: string }} data
 * @returns {Promise<object>} aporte creado
 */
export async function registrarAporte(data) {
  const res = await fetch(BASE_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  })
  if (!res.ok) await lanzarErrorDesde(res)
  return res.json()
}

/**
 * Consulta el consolidado de aportes de un afiliado en un periodo.
 * @param {{ afiliadoId: string, periodoDesde: string, periodoHasta: string }} params
 * @returns {Promise<object>} consolidado con total y detalle
 */
export async function consultarConsolidado({ afiliadoId, periodoDesde, periodoHasta }) {
  const qs = new URLSearchParams({ afiliadoId, periodoDesde, periodoHasta })
  const res = await fetch(`${BASE_URL}/consolidado?${qs}`)
  if (!res.ok) await lanzarErrorDesde(res)
  return res.json()
}

/**
 * Aprueba o rechaza un aporte marcado para revisión.
 * @param {number} id
 * @param {'aprobar' | 'rechazar'} accion
 * @returns {Promise<object>} aporte actualizado
 */
export async function resolverAporte(id, accion) {
  const res = await fetch(`${BASE_URL}/${id}/${accion}`, { method: 'POST' })
  if (!res.ok) await lanzarErrorDesde(res)
  return res.json()
}

const CONFIG_URL = '/api/configuracion/parametros'

/**
 * Obtiene los parámetros globales (tope mensual y umbral de revisión).
 * @returns {Promise<{ topeMensual: number, umbralRevision: number }>}
 */
export async function obtenerParametros() {
  const res = await fetch(CONFIG_URL)
  if (!res.ok) await lanzarErrorDesde(res)
  return res.json()
}

/**
 * Actualiza los parámetros globales.
 * @param {{ topeMensual: number, umbralRevision: number }} data
 * @returns {Promise<{ topeMensual: number, umbralRevision: number }>}
 */
export async function actualizarParametros(data) {
  const res = await fetch(CONFIG_URL, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  })
  if (!res.ok) await lanzarErrorDesde(res)
  return res.json()
}
