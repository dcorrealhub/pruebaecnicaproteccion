import { useState } from 'react'
import { consultarConsolidado } from '../api/aportesApi'

/**
 * TODO (candidato): implementar la vista de consolidado de aportes.
 *
 * Campos de búsqueda:
 *   - afiliadoId (texto)
 *   - periodoDesde (formato YYYY-MM)
 *   - periodoHasta (formato YYYY-MM)
 *
 * Resultado esperado:
 *   - Total aportado en el periodo
 *   - Tabla con el detalle de cada aporte (fecha, monto, canal, marcadaRevision)
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
          Periodo desde (YYYY-MM)
          <input
            value={filtros.periodoDesde}
            onChange={e => setFiltros(f => ({ ...f, periodoDesde: e.target.value }))}
            placeholder="2025-01"
            pattern="\d{4}-\d{2}"
            required
            style={{ display: 'block', marginTop: 4 }}
          />
        </label>

        <label>
          Periodo hasta (YYYY-MM)
          <input
            value={filtros.periodoHasta}
            onChange={e => setFiltros(f => ({ ...f, periodoHasta: e.target.value }))}
            placeholder="2025-06"
            pattern="\d{4}-\d{2}"
            required
            style={{ display: 'block', marginTop: 4 }}
          />
        </label>

        <button type="submit" disabled={cargando}>
          {cargando ? 'Consultando...' : 'Consultar'}
        </button>
      </form>

      {error && <p style={{ color: 'red' }}>Error: {error}</p>}

      {consolidado && (
        <div>
          <p><strong>Total aportado:</strong> {consolidado.totalAportado?.toLocaleString('es-CO', { style: 'currency', currency: 'COP' })}</p>

          {consolidado.detalle?.length > 0 ? (
            <table style={{ width: '100%', borderCollapse: 'collapse', marginTop: 12 }}>
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
                    <td style={td}>{a.monto?.toLocaleString('es-CO')}</td>
                    <td style={td}>{a.canal}</td>
                    <td style={td}>{a.marcadaRevision ? 'Sí' : 'No'}</td>
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
