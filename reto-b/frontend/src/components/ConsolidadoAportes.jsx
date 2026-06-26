import { useState } from 'react'
import { consultarConsolidado } from '../api/aportesApi'

const mesActual = new Date().toISOString().slice(0, 7)

export default function ConsolidadoAportes() {
  const [filtros, setFiltros] = useState({ afiliadoId: '', periodoDesde: '', periodoHasta: '' })
  const [consolidado, setConsolidado] = useState(null)
  const [error, setError] = useState(null)
  const [cargando, setCargando] = useState(false)

  async function handleBuscar(e) {
    e.preventDefault()
    if (filtros.periodoDesde > filtros.periodoHasta) {
      setError('El periodo "desde" no puede ser posterior al periodo "hasta".')
      return
    }
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
    <div>
      <h2>Consolidado de aportes</h2>

      <form onSubmit={handleBuscar} className="form-row" style={{ marginBottom: 24 }}>
        <div className="field">
          <label>ID Afiliado</label>
          <input
            value={filtros.afiliadoId}
            onChange={e => setFiltros(f => ({ ...f, afiliadoId: e.target.value.toUpperCase() }))}
            placeholder="AF-001"
            required
          />
        </div>

        <div className="field">
          <label>Periodo desde</label>
          <input
            type="month"
            value={filtros.periodoDesde}
            onChange={e => setFiltros(f => ({ ...f, periodoDesde: e.target.value }))}
            max={filtros.periodoHasta || mesActual}
            required
          />
        </div>

        <div className="field">
          <label>Periodo hasta</label>
          <input
            type="month"
            value={filtros.periodoHasta}
            onChange={e => setFiltros(f => ({ ...f, periodoHasta: e.target.value }))}
            min={filtros.periodoDesde}
            max={mesActual}
            required
          />
        </div>

        <button type="submit" className="btn-primary" disabled={cargando}>
          {cargando ? 'Consultando...' : 'Consultar'}
        </button>
      </form>

      {error && (
        <div className="alert alert-error">
          <span>✕</span>
          <span>{error}</span>
        </div>
      )}

      {consolidado && (
        <div>
          <div className="result-header">
            <span>Total aportado en el periodo:</span>
            <span className="stat-chip">
              {consolidado.totalAportado?.toLocaleString('es-CO', { style: 'currency', currency: 'COP' })}
            </span>
          </div>

          {consolidado.detalle?.length > 0 ? (
            <table className="data-table">
              <thead>
                <tr>
                  <th>Fecha</th>
                  <th>Monto (COP)</th>
                  <th>Canal</th>
                  <th>Revisión</th>
                </tr>
              </thead>
              <tbody>
                {consolidado.detalle.map(a => (
                  <tr key={a.id}>
                    <td>{a.fecha}</td>
                    <td>{a.monto?.toLocaleString('es-CO')}</td>
                    <td>{a.canal}</td>
                    <td>
                      <span className={`badge ${a.marcadaRevision ? 'badge-yes' : 'badge-no'}`}>
                        {a.marcadaRevision ? 'Sí' : 'No'}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <p style={{ color: 'var(--color-muted)', fontSize: 14 }}>
              No se encontraron aportes en el periodo indicado.
            </p>
          )}
        </div>
      )}
    </div>
  )
}
