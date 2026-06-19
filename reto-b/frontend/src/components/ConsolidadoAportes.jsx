import { useState } from 'react'
import { consultarConsolidado } from '../api/aportesApi'
import './aportes.css'

const formatoCOP = new Intl.NumberFormat('es-CO', {
  style: 'currency',
  currency: 'COP',
  maximumFractionDigits: 0,
})

const formatoFecha = new Intl.DateTimeFormat('es-CO', {
  day: '2-digit',
  month: 'short',
  year: 'numeric',
})

function formatearMonto(valor) {
  const numero = Number(valor)
  return Number.isFinite(numero) ? formatoCOP.format(numero) : '—'
}

function formatearFecha(valor) {
  if (!valor) return '—'
  const fecha = new Date(valor)
  return Number.isNaN(fecha.getTime()) ? valor : formatoFecha.format(fecha)
}

/**
 * Vista de consolidado de aportes de un afiliado en un periodo.
 * Las filas marcadas para revisión se resaltan para que el operador
 * las identifique sin tener que leer cada celda.
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
    <div className="ap-root">
      <div className="ap-card">
        <div className="ap-heading">
          <h2>Consolidado de aportes</h2>
          <span className="ap-eyebrow">Por afiliado y periodo</span>
        </div>

        <form onSubmit={handleBuscar}>
          <div className="ap-filters">
            <div className="ap-field">
              <label htmlFor="afiliadoId">ID afiliado</label>
              <input
                id="afiliadoId"
                className="ap-input"
                value={filtros.afiliadoId}
                onChange={e => setFiltros(f => ({ ...f, afiliadoId: e.target.value }))}
                placeholder="AF-001"
                required
              />
            </div>

            <div className="ap-field">
              <label htmlFor="periodoDesde">Desde <span className="ap-hint">AAAA-MM</span></label>
              <input
                id="periodoDesde"
                className="ap-input"
                value={filtros.periodoDesde}
                onChange={e => setFiltros(f => ({ ...f, periodoDesde: e.target.value }))}
                placeholder="2025-01"
                pattern="\d{4}-\d{2}"
                required
              />
            </div>

            <div className="ap-field">
              <label htmlFor="periodoHasta">Hasta <span className="ap-hint">AAAA-MM</span></label>
              <input
                id="periodoHasta"
                className="ap-input"
                value={filtros.periodoHasta}
                onChange={e => setFiltros(f => ({ ...f, periodoHasta: e.target.value }))}
                placeholder="2025-06"
                pattern="\d{4}-\d{2}"
                required
              />
            </div>

            <button type="submit" className="ap-button" disabled={cargando}>
              {cargando ? 'Consultando…' : 'Consultar'}
            </button>
          </div>
        </form>

        {error && (
          <div className="ap-alert ap-alert-error" role="alert">
            <svg className="ap-alert-icon" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
              <circle cx="10" cy="10" r="8.5" stroke="currentColor" strokeWidth="1.4" />
              <path d="M10 6v4.5" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" />
              <circle cx="10" cy="13.5" r="0.9" fill="currentColor" />
            </svg>
            <span><strong>No se pudo consultar el consolidado.</strong> {error}</span>
          </div>
        )}

        {consolidado && (
          <>
            <div className="ap-summary">
              <span className="ap-summary-label">Total aportado</span>
              <span className="ap-summary-value">{formatearMonto(consolidado.totalAportado)}</span>
            </div>

            {consolidado.detalle?.length > 0 ? (
              <table className="ap-table">
                <thead>
                  <tr>
                    <th>Fecha</th>
                    <th className="ap-col-monto">Monto</th>
                    <th>Canal</th>
                    <th>Estado</th>
                  </tr>
                </thead>
                <tbody>
                  {consolidado.detalle.map(a => (
                    <tr key={a.id} className={a.marcadaRevision ? 'ap-row-revision' : ''}>
                      <td>{formatearFecha(a.fecha)}</td>
                      <td className="ap-col-monto">{formatearMonto(a.monto)}</td>
                      <td>{a.canal}</td>
                      <td>
                        {a.marcadaRevision ? (
                          <span className="ap-badge ap-badge-revision">En revisión</span>
                        ) : (
                          <span className="ap-badge ap-badge-ok">—</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <p className="ap-empty">No se encontraron aportes en el periodo indicado.</p>
            )}
          </>
        )}
      </div>
    </div>
  )
}