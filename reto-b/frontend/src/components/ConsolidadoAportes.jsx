import { useState } from 'react'
import { consultarConsolidado } from '../api/aportesApi'

const PERIODO_ACTUAL = new Date().toISOString().slice(0, 7)
const PERIODO_INICIO_ANO = `${new Date().getFullYear()}-01`

function formatCOP(valor) {
  return Number(valor).toLocaleString('es-CO', { style: 'currency', currency: 'COP', minimumFractionDigits: 0 })
}

export default function ConsolidadoAportes() {
  const [filtros, setFiltros] = useState({
    afiliadoId: '',
    periodoDesde: PERIODO_INICIO_ANO,
    periodoHasta: PERIODO_ACTUAL,
  })
  const [consolidado, setConsolidado] = useState(null)
  const [error, setError] = useState(null)
  const [cargando, setCargando] = useState(false)

  function campo(nombre) {
    return {
      value: filtros[nombre],
      onChange: e => setFiltros(f => ({ ...f, [nombre]: e.target.value })),
    }
  }

  async function handleBuscar(e) {
    e.preventDefault()
    setError(null)
    setConsolidado(null)
    setCargando(true)
    try {
      const data = await consultarConsolidado(filtros)
      setConsolidado(data)
    } catch (err) {
      setError(err.message)
    } finally {
      setCargando(false)
    }
  }

  return (
    <div className="card">
      <h2 className="card-title">Consolidado de aportes</h2>

      <form onSubmit={handleBuscar} noValidate>
        <div className="filter-bar">
          <div className="form-field">
            <label className="form-label" htmlFor="c-afiliado">ID Afiliado</label>
            <input
              id="c-afiliado"
              className="form-input"
              placeholder="AF-001"
              required
              data-testid="input-c-afiliado"
              {...campo('afiliadoId')}
            />
          </div>

          <div className="form-field">
            <label className="form-label" htmlFor="c-desde">Periodo desde</label>
            <input
              id="c-desde"
              type="month"
              className="form-input"
              required
              data-testid="input-c-desde"
              {...campo('periodoDesde')}
            />
          </div>

          <div className="form-field">
            <label className="form-label" htmlFor="c-hasta">Periodo hasta</label>
            <input
              id="c-hasta"
              type="month"
              className="form-input"
              required
              data-testid="input-c-hasta"
              {...campo('periodoHasta')}
            />
          </div>

          <button type="submit" className="btn btn-primary" disabled={cargando} data-testid="btn-consultar">
            {cargando ? 'Consultando…' : 'Consultar'}
          </button>
        </div>
      </form>

      {error && <div className="alert alert-error" role="alert">{error}</div>}

      {consolidado && (
        <div>
          <div className="total-banner">
            <span className="total-label">Total aportado en el periodo</span>
            <span className="total-value">{formatCOP(consolidado.totalAportado)}</span>
          </div>

          {consolidado.detalle?.length > 0 ? (
            <table className="data-table" aria-label="Detalle de aportes">
              <thead>
                <tr>
                  <th>Fecha</th>
                  <th>Monto</th>
                  <th>Canal</th>
                  <th>Estado</th>
                </tr>
              </thead>
              <tbody>
                {consolidado.detalle.map(a => (
                  <tr key={a.id}>
                    <td>{a.fecha}</td>
                    <td>{formatCOP(a.monto)}</td>
                    <td>{a.canal.replace('_', ' ')}</td>
                    <td>
                      {a.marcadaRevision
                        ? <span className="badge-revision" title="Este aporte requiere revisión por cumplimiento">⚠ Revisión</span>
                        : <span style={{ color: 'var(--color-success)' }}>✓ Normal</span>
                      }
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <p className="empty-state">No se encontraron aportes en el periodo indicado.</p>
          )}
        </div>
      )}
    </div>
  )
}
