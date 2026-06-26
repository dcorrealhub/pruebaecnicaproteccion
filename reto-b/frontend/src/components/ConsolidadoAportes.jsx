import { useState } from 'react'
import { consultarConsolidado, resolverAporte } from '../api/aportesApi'

const COP = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 2 })

/** Devuelve un periodo YYYY-MM desplazado `mesesAtras` respecto del mes actual. */
function periodo(mesesAtras = 0) {
  const d = new Date()
  d.setDate(1) // evita saltos por días inexistentes (ej: 31)
  d.setMonth(d.getMonth() - mesesAtras)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}

// Rango por defecto: del mismo mes del año anterior hasta el mes actual (1 año).
const FILTROS_INICIALES = { afiliadoId: '', periodoDesde: periodo(12), periodoHasta: periodo(0) }

/**
 * Vista de consolidado: busca aportes por afiliado y rango de periodos, muestra el
 * total aprobado y el total en revisión, y permite resolver (aprobar/rechazar) los
 * aportes pendientes desde la misma tabla.
 */
export default function ConsolidadoAportes() {
  const [filtros, setFiltros] = useState(FILTROS_INICIALES)
  const [consolidado, setConsolidado] = useState(null)
  const [error, setError] = useState(null)
  const [cargando, setCargando] = useState(false)
  const [resolviendo, setResolviendo] = useState(null) // id en proceso

  async function buscar() {
    setError(null)
    setCargando(true)
    try {
      const data = await consultarConsolidado(filtros)
      setConsolidado(data)
    } catch (err) {
      setError(err.message)
      setConsolidado(null)
    } finally {
      setCargando(false)
    }
  }

  async function handleBuscar(e) {
    e.preventDefault()
    await buscar()
  }

  async function handleResolver(id, accion) {
    setError(null)
    setResolviendo(id)
    try {
      await resolverAporte(id, accion)
      await buscar() // refresca totales y estados
    } catch (err) {
      setError(err.message)
    } finally {
      setResolviendo(null)
    }
  }

  return (
    <div>
      <h2 style={{ fontSize: 18, marginBottom: 16 }}>Consolidado de aportes</h2>

      <form onSubmit={handleBuscar} style={{ display: 'flex', gap: 12, flexWrap: 'wrap', alignItems: 'flex-end', marginBottom: 24 }}>
        <label>
          ID Afiliado
          <input
            value={filtros.afiliadoId}
            onChange={e => setFiltros(f => ({ ...f, afiliadoId: e.target.value }))}
            placeholder="AF-001"
            required
            style={{ display: 'block', marginTop: 4 }}
          />
        </label>

        <label>
          Periodo desde
          <input
            type="month"
            value={filtros.periodoDesde}
            max={filtros.periodoHasta || undefined}
            onChange={e => setFiltros(f => ({ ...f, periodoDesde: e.target.value }))}
            required
            style={{ display: 'block', marginTop: 4 }}
          />
        </label>

        <label>
          Periodo hasta
          <input
            type="month"
            value={filtros.periodoHasta}
            min={filtros.periodoDesde || undefined}
            onChange={e => setFiltros(f => ({ ...f, periodoHasta: e.target.value }))}
            required
            style={{ display: 'block', marginTop: 4 }}
          />
        </label>

        <button type="submit" disabled={cargando}>
          {cargando ? 'Consultando...' : 'Consultar'}
        </button>
      </form>

      {error && <p style={{ color: 'crimson' }}>Error: {error}</p>}

      {consolidado && (
        <div>
          <p><strong>Total aportado (aprobado):</strong> {COP.format(consolidado.totalAportado ?? 0)}</p>
          {consolidado.totalEnRevision > 0 && (
            <p style={{ color: '#b8860b' }}>
              <strong>En revisión (reserva cupo del tope):</strong> {COP.format(consolidado.totalEnRevision)}
            </p>
          )}
          <p>
            <strong>Total comprometido (aprobado + en revisión):</strong>{' '}
            {COP.format((consolidado.totalAportado ?? 0) + (consolidado.totalEnRevision ?? 0))}
          </p>

          {consolidado.detalle?.length > 0 ? (
            <table style={{ width: '100%', borderCollapse: 'collapse', marginTop: 12 }}>
              <thead>
                <tr style={{ background: '#eee' }}>
                  <th style={th}>Fecha</th>
                  <th style={th}>Monto</th>
                  <th style={th}>Canal</th>
                  <th style={th}>Estado</th>
                  <th style={th}>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {consolidado.detalle.map(a => (
                  <tr key={a.id}>
                    <td style={td}>{a.fecha}</td>
                    <td style={td}>{COP.format(a.monto)}</td>
                    <td style={td}>{a.canal}</td>
                    <td style={td}>{a.estado}</td>
                    <td style={td}>
                      {a.estado === 'PENDIENTE_REVISION' ? (
                        <span style={{ display: 'flex', gap: 6 }}>
                          <button disabled={resolviendo === a.id} onClick={() => handleResolver(a.id, 'aprobar')}>
                            Aprobar
                          </button>
                          <button disabled={resolviendo === a.id} onClick={() => handleResolver(a.id, 'rechazar')}>
                            Rechazar
                          </button>
                        </span>
                      ) : '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <p>No se encontraron aportes en el periodo indicado.</p>
          )}
        </div>
      )}
    </div>
  )
}

const th = { padding: '8px 12px', textAlign: 'left', borderBottom: '1px solid #ccc' }
const td = { padding: '8px 12px', borderBottom: '1px solid #eee' }
