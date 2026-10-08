import { useState } from 'react'
import { consultarConsolidado } from '../api/aportesApi'
import { etiquetaCanal, formatearCOP } from '../formato'

const PERIODO = /^\d{4}-(0[1-9]|1[0-2])$/

/**
 * Consulta del consolidado de aportes de un afiliado en un rango de periodos (inclusive).
 */
export default function ConsolidadoAportes() {
  const [filtros, setFiltros] = useState({ afiliadoId: '', periodoDesde: '', periodoHasta: '' })
  const [consolidado, setConsolidado] = useState(null)
  const [error, setError] = useState(null)
  const [cargando, setCargando] = useState(false)

  async function handleBuscar(e) {
    e.preventDefault()
    setError(null)
    setConsolidado(null)

    const { periodoDesde, periodoHasta } = filtros
    if (!filtros.afiliadoId.trim()) {
      setError('Ingresa el ID del afiliado')
      return
    }
    if (!PERIODO.test(periodoDesde) || !PERIODO.test(periodoHasta)) {
      setError('Los periodos deben tener formato YYYY-MM')
      return
    }
    // YYYY-MM se ordena correctamente como texto
    if (periodoDesde > periodoHasta) {
      setError('El periodo desde no puede ser posterior al periodo hasta')
      return
    }

    setCargando(true)
    try {
      const data = await consultarConsolidado({ ...filtros, afiliadoId: filtros.afiliadoId.trim() })
      setConsolidado(data)
    } catch (err) {
      setError(err.message)
    } finally {
      setCargando(false)
    }
  }

  const detalle = consolidado?.detalle ?? []
  const marcados = detalle.filter(a => a.marcadaRevision).length

  return (
    <>
      <section className="card">
        <div className="card__header">
          <h2 className="card__title">Consolidado de aportes</h2>
          <p className="card__subtitle">Consulta el total y el detalle de los aportes de un afiliado en un rango de periodos.</p>
        </div>

        <form onSubmit={handleBuscar} noValidate className="form-grid form-grid--filters">
          <label className="field">
            <span className="field__label">ID del afiliado</span>
            <input
              className="input"
              value={filtros.afiliadoId}
              onChange={e => setFiltros(f => ({ ...f, afiliadoId: e.target.value }))}
              placeholder="AF-001"
              maxLength={50}
              autoComplete="off"
            />
          </label>

          <label className="field">
            <span className="field__label">Desde</span>
            <input
              className="input"
              type="month"
              value={filtros.periodoDesde}
              onChange={e => setFiltros(f => ({ ...f, periodoDesde: e.target.value }))}
              placeholder="2026-01"
            />
          </label>

          <label className="field">
            <span className="field__label">Hasta</span>
            <input
              className="input"
              type="month"
              value={filtros.periodoHasta}
              onChange={e => setFiltros(f => ({ ...f, periodoHasta: e.target.value }))}
              placeholder="2026-06"
            />
          </label>

          <button type="submit" className="btn btn--primary" disabled={cargando}>
            {cargando && <span className="spinner" aria-hidden="true" />}
            {cargando ? 'Consultando…' : 'Consultar'}
          </button>
        </form>

        {error && (
          <div role="alert" className="alert alert--error">
            <span className="alert__icon" aria-hidden="true">!</span>
            <div>
              <p className="alert__title">No se pudo consultar</p>
              <p className="alert__body">{error}</p>
            </div>
          </div>
        )}
      </section>

      {consolidado && (
        <section className="card">
          <div className="card__header">
            <h2 className="card__title">Afiliado {consolidado.afiliadoId}</h2>
            <p className="card__subtitle">Periodo {consolidado.periodoDesde} a {consolidado.periodoHasta}</p>
          </div>

          <div className="stats">
            <div className="stat stat--primary">
              <p className="stat__label">Total aportado</p>
              <p className="stat__value">{formatearCOP(consolidado.totalAportado)}</p>
            </div>
            <div className="stat">
              <p className="stat__label">Aportes</p>
              <p className="stat__value">{detalle.length}</p>
            </div>
            <div className={`stat ${marcados > 0 ? 'stat--warning' : ''}`}>
              <p className="stat__label">Pendientes de revisión</p>
              <p className="stat__value">{marcados}</p>
            </div>
          </div>

          {detalle.length > 0 ? (
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Fecha</th>
                    <th>Periodo</th>
                    <th>Canal</th>
                    <th>Revisión</th>
                    <th className="num">Monto</th>
                  </tr>
                </thead>
                <tbody>
                  {detalle.map(a => (
                    <tr key={a.id}>
                      <td>{a.fecha}</td>
                      <td>{a.periodo}</td>
                      <td><span className="badge badge--neutral">{etiquetaCanal(a.canal)}</span></td>
                      <td>
                        {a.marcadaRevision
                          ? <span className="badge badge--warning">Pendiente</span>
                          : <span className="badge badge--success">No requiere</span>}
                      </td>
                      <td className="num">{formatearCOP(a.monto)}</td>
                    </tr>
                  ))}
                </tbody>
                <tfoot>
                  <tr>
                    <td colSpan={4}>Total</td>
                    <td className="num">{formatearCOP(consolidado.totalAportado)}</td>
                  </tr>
                </tfoot>
              </table>
            </div>
          ) : (
            <div className="empty">
              <p className="empty__title">Sin aportes en este periodo</p>
              <p>Prueba con otro rango de meses o con otro afiliado.</p>
            </div>
          )}
        </section>
      )}
    </>
  )
}
