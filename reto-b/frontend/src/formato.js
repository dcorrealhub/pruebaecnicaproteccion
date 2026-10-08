const COP = new Intl.NumberFormat('es-CO', {
  style: 'currency',
  currency: 'COP',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

/** Solo para mostrar: los cálculos de dinero se hacen en el backend con BigDecimal. */
export function formatearCOP(valor) {
  if (valor === null || valor === undefined) return ''
  return COP.format(Number(valor))
}

/**
 * Convierte lo que el usuario escribe en formato es-CO ("1.500.000,5") al valor canónico
 * que se envía al backend ("1500000.5"). El punto se ignora (separador de miles) y la coma
 * marca los decimales; se conservan máximo 13 enteros y 2 decimales.
 */
export function normalizarMonto(texto) {
  const limpio = texto.replace(/[^\d,]/g, '')
  const [entero = '', ...resto] = limpio.split(',')
  const enteroLimpio = entero.replace(/^0+(?=\d)/, '').slice(0, 13)
  if (!limpio.includes(',')) return enteroLimpio
  return `${enteroLimpio}.${resto.join('').slice(0, 2)}`
}

/** Valor canónico ("1500000.5") a texto con separadores es-CO ("1.500.000,5"). */
export function mostrarMonto(canonico) {
  if (!canonico) return ''
  const [entero, decimales] = canonico.split('.')
  const conMiles = entero.replace(/\B(?=(\d{3})+(?!\d))/g, '.')
  return decimales === undefined ? conMiles : `${conMiles},${decimales}`
}

export const CANALES = [
  { valor: 'APP_MOVIL', etiqueta: 'App móvil' },
  { valor: 'WEB', etiqueta: 'Web' },
  { valor: 'SUCURSAL', etiqueta: 'Sucursal' },
]

export function etiquetaCanal(valor) {
  return CANALES.find(c => c.valor === valor)?.etiqueta ?? valor
}
