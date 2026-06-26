import { useState } from 'react'
import { consultarConsolidado } from '../api/aportesApi'
import Swal from 'sweetalert2'

export default function ConsolidadoAportes() {
  const [filtros, setFiltros] = useState({ afiliadoId: '', periodoDesde: '', periodoHasta: '' })
  const [consolidado, setConsolidado] = useState(null)
  const [cargando, setCargando] = useState(false)

  async function handleBuscar(e) {
    e.preventDefault()
    setConsolidado(null)
    setCargando(true)

    try {
      const data = await consultarConsolidado(filtros)
      setConsolidado(data)

      if (!data.detalle || data.detalle.length === 0) {
        Swal.fire({
          title: 'Sin Resultados',
          text: 'No se encontraron aportes para el afiliado en el periodo indicado.',
          icon: 'info',
          confirmButtonText: 'Aceptar',
          customClass: {
            popup: 'custom-swal-popup',
            title: 'custom-swal-title',
            confirmButton: 'custom-swal-confirm'
          }
        })
      }
    } catch (err) {
      Swal.fire({
        title: 'Error de Consulta',
        text: err.message || 'No se pudo obtener el consolidado de aportes.',
        icon: 'error',
        confirmButtonText: 'Aceptar',
        customClass: {
          popup: 'custom-swal-popup',
          title: 'custom-swal-title',
          confirmButton: 'custom-swal-confirm'
        }
      })
    } finally {
      setCargando(false)
    }
  }

  return (
    <div className="card fade-in" style={{ width: '100%' }}>
      <h2 className="card-title">Consolidado de Aportes</h2>

      {/* Filter Form */}
      <form onSubmit={handleBuscar} style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
        gap: '1rem',
        alignItems: 'flex-end',
        marginBottom: '2rem',
        padding: '1.25rem',
        backgroundColor: '#f8fafc',
        borderRadius: 'var(--radius-md)',
        border: '1px solid var(--border-light)'
      }}>
        <div className="form-group" style={{ marginBottom: 0 }}>
          <label className="form-label">ID Afiliado</label>
          <input
            value={filtros.afiliadoId}
            onChange={e => setFiltros(f => ({ ...f, afiliadoId: e.target.value }))}
            placeholder="Ej: AF-001"
            className="form-input"
            required
            disabled={cargando}
          />
        </div>

        <div className="form-group" style={{ marginBottom: 0 }}>
          <label className="form-label">Periodo Desde (YYYY-MM)</label>
          <input
            value={filtros.periodoDesde}
            onChange={e => setFiltros(f => ({ ...f, periodoDesde: e.target.value }))}
            placeholder="2025-01"
            pattern="\d{4}-\d{2}"
            className="form-input"
            required
            disabled={cargando}
          />
        </div>

        <div className="form-group" style={{ marginBottom: 0 }}>
          <label className="form-label">Periodo Hasta (YYYY-MM)</label>
          <input
            value={filtros.periodoHasta}
            onChange={e => setFiltros(f => ({ ...f, periodoHasta: e.target.value }))}
            placeholder="2025-06"
            pattern="\d{4}-\d{2}"
            className="form-input"
            required
            disabled={cargando}
          />
        </div>

        <button type="submit" className="btn btn-primary" disabled={cargando}>
          {cargando ? 'Consultando...' : '🔍 Buscar'}
        </button>
      </form>

      {/* Results Display */}
      {consolidado && (
        <div className="fade-in">
          {/* Total Box */}
          <div className="stat-card" style={{ maxWidth: '320px', marginBottom: '1.5rem', border: '1.5px solid var(--color-success)' }}>
            <div className="stat-icon" style={{ backgroundColor: 'var(--color-success-bg)', color: 'var(--color-success)' }}>💰</div>
            <div className="stat-info">
              <span className="stat-label">Total Aportado en Periodo</span>
              <span className="stat-value" style={{ color: 'var(--color-success)' }}>
                {consolidado.totalAportado?.toLocaleString('es-CO', { style: 'currency', currency: 'COP' })}
              </span>
            </div>
          </div>

          {/* Details Table */}
          {consolidado.detalle && consolidado.detalle.length > 0 ? (
            <div>
              <h3 style={{ fontSize: '1.1rem', marginBottom: '0.75rem', fontWeight: 600 }}>Detalle de Transacciones</h3>
              <div className="table-container">
                <table className="custom-table">
                  <thead>
                    <tr>
                      <th>Fecha</th>
                      <th>Monto</th>
                      <th>Canal</th>
                      <th>Estado / Revisión</th>
                    </tr>
                  </thead>
                  <tbody>
                    {consolidado.detalle.map(a => (
                      <tr key={a.id}>
                        <td style={{ fontWeight: 500 }}>{a.fecha}</td>
                        <td style={{ fontFamily: 'monospace', fontWeight: 600 }}>
                          $ {a.monto?.toLocaleString('es-CO', { minimumFractionDigits: 2 })}
                        </td>
                        <td>
                          {a.canal === 'APP_MOVIL' ? '📲 App móvil' : a.canal === 'WEB' ? '💻 Web' : '🏢 Sucursal'}
                        </td>
                        <td>
                          <span className={`badge ${a.marcadaRevision ? 'badge-warning' : 'badge-success'}`}>
                            {a.marcadaRevision ? '⚠️ En revisión' : '✅ Procesado'}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          ) : (
            <div className="text-center text-muted" style={{ padding: '2rem', border: '1px dashed var(--border-light)', borderRadius: 'var(--radius-md)' }}>
              No se encontraron aportes registrados en este periodo.
            </div>
          )}
        </div>
      )}
    </div>
  )
}

