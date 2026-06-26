const BASE_URL = '/api/aportes'

export async function registrarAporte({ afiliadoId, monto, canal, idempotenciaKey }) {
  const res = await fetch(BASE_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ afiliadoId, monto, canal, idempotenciaKey }),
  })
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.mensaje || `Error ${res.status}`)
  }
  return res.json()
}

export async function consultarConsolidado({ afiliadoId, periodoDesde, periodoHasta }) {
  const params = new URLSearchParams({ afiliadoId, periodoDesde, periodoHasta })
  const res = await fetch(`${BASE_URL}/consolidado?${params}`)
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.mensaje || `Error ${res.status}`)
  }
  return res.json()
}

export async function cambiarEstadoAporte(id, { nuevoEstado, revisor, comentario }) {
  const res = await fetch(`${BASE_URL}/${id}/estado`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ nuevoEstado, revisor, comentario }),
  })
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.mensaje || err.message || `Error ${res.status}`)
  }
  return res.json()
}

export async function consultarRevisiones(id) {
  const res = await fetch(`${BASE_URL}/${id}/revisiones`)
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.mensaje || `Error ${res.status}`)
  }
  return res.json()
}
