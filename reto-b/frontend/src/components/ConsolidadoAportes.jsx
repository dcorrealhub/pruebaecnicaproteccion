import { useState } from 'react'
import { consultarConsolidado } from '../api/aportesApi'

function formatCOP(value) {
  return Number(value).toLocaleString('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })
}

const CANAL_LABELS = { APP_MOVIL: 'App movil', WEB: 'Web', SUCURSAL: 'Sucursal' }

export default function ConsolidadoAportes() {
  const [filtros, setFiltros] = useState({ afiliadoId: '', periodoDesde: '', periodoHasta: '' })
  const [erroresCampo, setErroresCampo] = useState({})
  const [consolidado, setConsolidado] = useState(null)
  const [error, setError] = useState(null)
  const [cargando, setCargando] = useState(false)

  const PERIODO_PATTERN = /^\d{4}-(0[1-9]|1[0-2])$/

  function validar() {
    const errs = {}
    if (!filtros.afiliadoId.trim()) errs.afiliadoId = 'Ingresa el ID del afiliado'
    if (!filtros.periodoDesde) errs.periodoDesde = 'Requerido'
    else if (!PERIODO_PATTERN.test(filtros.periodoDesde)) errs.periodoDesde = 'Formato YYYY-MM'
    if (!filtros.periodoHasta) errs.periodoHasta = 'Requerido'
    else if (!PERIODO_PATTERN.test(filtros.periodoHasta)) errs.periodoHasta = 'Formato YYYY-MM'
    if (!errs.periodoDesde && !errs.periodoHasta && filtros.periodoHasta < filtros.periodoDesde)
      errs.periodoHasta = 'Debe ser >= periodo desde'
    return errs
  }

  function handleChange(campo, valor) {
    setFiltros(f => ({ ...f, [campo]: valor }))
    if (erroresCampo[campo]) setErroresCampo(e => ({ ...e, [campo]: undefined }))
  }

  async function handleBuscar(e) {
    e.preventDefault()
    setError(null)
    setConsolidado(null)

    const errs = validar()
    if (Object.keys(errs).length > 0) { setErroresCampo(errs); return }

    setCargando(true)
    try {
      const data = await consultarConsolidado(filtros)
      setConsolidado(data)
    } catch (err) {
      setError(err.message || 'Error al consultar')
    } finally {
      setCargando(false)
    }
  }

  return (
    <div className="card">
      <h2 className="card__title">Consolidado de aportes</h2>

      <form onSubmit={handleBuscar} noValidate>
        <div className="search-bar">
          <div className="form-field">
            <label className="form-label" htmlFor="afiliadoId">ID Afiliado</label>
            <input
              id="afiliadoId"
              className={`form-input ${erroresCampo.afiliadoId ? 'form-input--error' : ''}`}
              value={filtros.afiliadoId}
              onChange={e => handleChange('afiliadoId', e.target.value)}
              placeholder="AF-001"
            />
            {erroresCampo.afiliadoId && <span className="field-error">{erroresCampo.afiliadoId}</span>}
          </div>

          <div className="form-field">
            <label className="form-label" htmlFor="periodoDesde">Desde (YYYY-MM)</label>
            <input
              id="periodoDesde"
              className={`form-input ${erroresCampo.periodoDesde ? 'form-input--error' : ''}`}
              value={filtros.periodoDesde}
              onChange={e => handleChange('periodoDesde', e.target.value)}
              placeholder="2026-01"
              maxLength={7}
            />
            {erroresCampo.periodoDesde && <span className="field-error">{erroresCampo.periodoDesde}</span>}
          </div>

          <div className="form-field">
            <label className="form-label" htmlFor="periodoHasta">Hasta (YYYY-MM)</label>
            <input
              id="periodoHasta"
              className={`form-input ${erroresCampo.periodoHasta ? 'form-input--error' : ''}`}
              value={filtros.periodoHasta}
              onChange={e => handleChange('periodoHasta', e.target.value)}
              placeholder="2026-12"
              maxLength={7}
            />
            {erroresCampo.periodoHasta && <span className="field-error">{erroresCampo.periodoHasta}</span>}
          </div>

          <button type="submit" className="btn btn--primary" disabled={cargando} style={{ alignSelf: 'flex-end', marginBottom: erroresCampo.periodoHasta ? 20 : 0 }}>
            {cargando ? 'Consultando...' : 'Consultar'}
          </button>
        </div>
      </form>

      {error && (
        <div className="alert alert--error" style={{ marginTop: 16 }}>
          <span className="alert__icon">✕</span>
          <div><span className="alert__title">Error al consultar</span>{error}</div>
        </div>
      )}

      {consolidado && (
        <>
          <div className="total-banner">
            <span className="total-banner__label">
              Total aportado — {consolidado.afiliadoId} · {consolidado.periodoDesde} a {consolidado.periodoHasta}
            </span>
            <span className="total-banner__amount">{formatCOP(consolidado.totalAportado)}</span>
          </div>

          {consolidado.detalle?.length > 0 ? (
            <div className="table-wrapper">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>#</th>
                    <th>Fecha</th>
                    <th>Monto</th>
                    <th>Canal</th>
                    <th>Periodo</th>
                    <th>Estado</th>
                  </tr>
                </thead>
                <tbody>
                  {consolidado.detalle.map((a, i) => (
                    <tr key={a.id}>
                      <td style={{ color: '#94a3b8' }}>{i + 1}</td>
                      <td>{a.fecha}</td>
                      <td style={{ fontWeight: 600 }}>{formatCOP(a.monto)}</td>
                      <td>{CANAL_LABELS[a.canal] ?? a.canal}</td>
                      <td>{a.periodo}</td>
                      <td>
                        <span className={`badge ${a.marcadaRevision ? 'badge--revision' : 'badge--ok'}`}>
                          {a.marcadaRevision ? 'Requiere revision' : 'Aprobado'}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="empty-state">
              <span className="empty-state__icon">📭</span>
              No se encontraron aportes en el periodo indicado.
            </div>
          )}
        </>
      )}
    </div>
  )
}
