export const CANALES = {
  APP_MOVIL: 'App móvil',
  WEB: 'Web',
  SUCURSAL: 'Sucursal',
}

export const formatoCOP = n =>
  Number(n).toLocaleString('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 2 })

/** 'yyyy-MM-dd' → '8 oct 2026' (se construye en hora local para no desplazar el día). */
export const formatoFecha = iso => {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(y, m - 1, d).toLocaleDateString('es-CO', { day: 'numeric', month: 'short', year: 'numeric' })
}

/** 'yyyy-MM' → 'octubre de 2026' */
export const formatoPeriodo = periodo => {
  const [y, m] = periodo.split('-').map(Number)
  return new Date(y, m - 1, 1).toLocaleDateString('es-CO', { month: 'long', year: 'numeric' })
}
