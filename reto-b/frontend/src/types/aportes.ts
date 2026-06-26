export interface RegistrarAporteRequest {
  afiliadoId: string
  monto: number
  canal: string
  idempotenciaKey: string
}

export interface AporteResponse {
  id: number
  afiliadoId: string
  monto: number
  fecha: string
  canal: string
  periodo: string
  marcadaRevision: boolean
}

export interface ConsolidadoResponse {
  afiliadoId: string
  periodoDesde: string
  periodoHasta: string
  totalAportado: number
  detalle: AporteResponse[]
}
