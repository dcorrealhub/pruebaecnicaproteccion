import { useState, useCallback } from 'react'
import { consultarConsolidado } from '../api/aportesApi'

const COLOR_ERROR = '#b91c1c'
const COLOR_MUTED = '#6b7280'
const PERIODO_REGEX = /^\d{4}-(0[1-9]|1[0-2])$/

const th = { padding: '8px 12px', textAlign: 'left', borderBottom: '1px solid #ccc' }
const td = { padding: '8px 12px', borderBottom: '1px solid #eee' }

function validarFiltros({ afiliadoId, periodoDesde, periodoHasta }) {
  const errores = {}
  if (!afiliadoId.trim()) {
    errores.afiliadoId = 'El ID del afiliado es obligatorio.'
  }
  if (!periodoDesde.trim()) {
    errores.periodoDesde = 'El periodo desde es obligatorio.'
  } else if (!PERIODO_REGEX.test(periodoDesde)) {
    errores.periodoDesde = 'Formato inválido. Use YYYY-MM (ej: 2025-01).'
  }
  if (!periodoHasta.trim()) {
    errores.periodoHasta = 'El periodo hasta es obligatorio.'
  } else if (!PERIODO_REGEX.test(periodoHasta)) {
    errores.periodoHasta = 'Formato inválido. Use YYYY-MM (ej: 2025-06).'
  }
  return errores
}

export default function ConsolidadoAportes() {
  const [filtros, setFiltros] = useState({ afiliadoId: '', periodoDesde: '', periodoHasta: '' })
  const [consolidado, setConsolidado] = useState(null)
  const [error, setError] = useState(null)
  const [cargando, setCargando] = useState(false)
  const [errores, setErrores] = useState({})
  const [consultado, setConsultado] = useState(false)

  const actualizarCampo = useCallback((campo, valor) => {
    setFiltros(f => ({ ...f, [campo]: valor }))
    setErrores(e => {
      if (!e[campo]) return e
      const next = { ...e }
      delete next[campo]
      return next
    })
  }, [])

  function handleBuscar(e) {
    e.preventDefault()
    setError(null)
    setConsolidado(null)
    setConsultado(false)

    const v = validarFiltros(filtros)
    setErrores(v)
    if (Object.keys(v).length > 0) return

    setCargando(true)

    consultarConsolidado(filtros)
      .then(data => {
        setConsolidado(data)
        setConsultado(true)
      })
      .catch(err => setError(err.message))
      .finally(() => setCargando(false))
  }

  return (
    <div>
      <h2 style={{ fontSize: 18, marginBottom: 16 }}>Consolidado de aportes</h2>

      <form onSubmit={handleBuscar} style={{ display: 'flex', gap: 12, flexWrap: 'wrap', alignItems: 'flex-end', marginBottom: 24 }}>
        <label>
          ID Afiliado
          <input
            value={filtros.afiliadoId}
            onChange={e => actualizarCampo('afiliadoId', e.target.value)}
            placeholder="AF-001"
            required
            style={{ display: 'block', marginTop: 4 }}
          />
          {errores.afiliadoId && (
            <span role="alert" style={{ display: 'block', color: COLOR_ERROR, fontSize: 13, marginTop: 2 }}>{errores.afiliadoId}</span>
          )}
        </label>

        <label>
          Periodo desde
          <input
            value={filtros.periodoDesde}
            onChange={e => actualizarCampo('periodoDesde', e.target.value)}
            placeholder="2025-01"
            pattern="\d{4}-\d{2}"
            required
            style={{ display: 'block', marginTop: 4 }}
          />
          {errores.periodoDesde && (
            <span role="alert" style={{ display: 'block', color: COLOR_ERROR, fontSize: 13, marginTop: 2 }}>{errores.periodoDesde}</span>
          )}
        </label>

        <label>
          Periodo hasta
          <input
            value={filtros.periodoHasta}
            onChange={e => actualizarCampo('periodoHasta', e.target.value)}
            placeholder="2025-06"
            pattern="\d{4}-\d{2}"
            required
            style={{ display: 'block', marginTop: 4 }}
          />
          {errores.periodoHasta && (
            <span role="alert" style={{ display: 'block', color: COLOR_ERROR, fontSize: 13, marginTop: 2 }}>{errores.periodoHasta}</span>
          )}
        </label>

        <button type="submit" disabled={cargando} style={{ alignSelf: 'flex-end' }}>
          {cargando ? 'Consultando...' : 'Consultar'}
        </button>
      </form>

      {error && (
        <p role="alert" style={{ color: COLOR_ERROR }}>{error}</p>
      )}

      {consultado && consolidado && (
        <div role="region" aria-label="Resultado del consolidado">
          <p>
            <strong>Total aportado:</strong>{' '}
            {consolidado.totalAportado?.toLocaleString('es-CO', { style: 'currency', currency: 'COP' })}
          </p>

          {consolidado.detalle?.length > 0 ? (
            <table style={{ width: '100%', borderCollapse: 'collapse', marginTop: 12 }}>
              <caption style={{ textAlign: 'left', marginBottom: 8, color: COLOR_MUTED }}>
                {consolidado.detalle.length} aporte{consolidado.detalle.length !== 1 ? 's' : ''} encontrado{consolidado.detalle.length !== 1 ? 's' : ''}
              </caption>
              <thead>
                <tr style={{ background: '#eee' }}>
                  <th style={th}>Fecha</th>
                  <th style={th}>Monto</th>
                  <th style={th}>Canal</th>
                  <th style={th}>Revisión</th>
                </tr>
              </thead>
              <tbody>
                {consolidado.detalle.map(a => (
                  <tr key={a.id}>
                    <td style={td}>{a.fecha}</td>
                    <td style={td}>{a.monto?.toLocaleString('es-CO', { style: 'currency', currency: 'COP' })}</td>
                    <td style={td}>{a.canal}</td>
                    <td style={td}>{a.marcadaRevision ? 'Sí' : 'No'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <p style={{ color: COLOR_MUTED }}>No se encontraron aportes en el periodo indicado.</p>
          )}
        </div>
      )}
    </div>
  )
}
