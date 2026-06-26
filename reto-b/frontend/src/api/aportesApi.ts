import type { RegistrarAporteRequest, AporteResponse, ConsolidadoResponse } from '../types/aportes'

export type { RegistrarAporteRequest, AporteResponse, ConsolidadoResponse }

const BACKEND = import.meta.env.VITE_BACKEND_URL ?? ''
const BASE_URL = `${BACKEND}/api/aportes`

let authToken: string | null = null

export function setToken(token: string) {
  authToken = token
}

function authHeader(): HeadersInit {
  return authToken ? { 'Authorization': `Bearer ${authToken}` } : {}
}

export async function registrarAporte(data: RegistrarAporteRequest): Promise<AporteResponse> {
  const response = await fetch(BASE_URL, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...authHeader(),
    },
    body: JSON.stringify(data),
  })

  if (!response.ok) {
    const body = await response.json().catch(() => ({})) as { error?: string }
    throw new Error(body.error || `Error ${response.status}`)
  }

  return response.json()
}

export async function consultarConsolidado({
  afiliadoId,
  periodoDesde,
  periodoHasta,
}: {
  afiliadoId: string
  periodoDesde: string
  periodoHasta: string
}): Promise<ConsolidadoResponse> {
  const params = new URLSearchParams({ afiliadoId, periodoDesde, periodoHasta })
  const response = await fetch(`${BASE_URL}/consolidado?${params}`, {
    headers: { ...authHeader() },
  })

  if (!response.ok) {
    const body = await response.json().catch(() => ({})) as { error?: string }
    throw new Error(body.error || `Error ${response.status}`)
  }

  return response.json()
}
