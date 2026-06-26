import { useState } from 'react'
import { consultarConsolidado } from '../api/aportesApi'

export default function ConsolidadoAportes() {
  const [filtros, setFiltros] = useState({ afiliadoId: '', periodoDesde: '', periodoHasta: '' })
  const [consolidado, setConsolidado] = useState(null)
  const [error, setError] = useState(null)
  const [cargando, setCargando] = useState(false)

  async function handleBuscar(e) {
    e.preventDefault()
    setError(null); setConsolidado(null); setCargando(true)
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
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>

      {/* Card de filtros */}
      <div style={s.card}>
        <p style={s.cardTitle}>Parámetros de búsqueda</p>
        <form onSubmit={handleBuscar}>
          <div style={s.grid}>
            <Field label="ID Afiliado" hint="Ej: AF-001">
              <input style={s.input}
                value={filtros.afiliadoId}
                onChange={e => setFiltros(f => ({ ...f, afiliadoId: e.target.value }))}
                placeholder="AF-001" required />
            </Field>

            <Field label="Periodo desde" hint="Formato YYYY-MM">
              <input style={s.input}
                value={filtros.periodoDesde}
                onChange={e => setFiltros(f => ({ ...f, periodoDesde: e.target.value }))}
                placeholder="2026-01"
                pattern="\d{4}-\d{2}" required />
            </Field>

            <Field label="Periodo hasta" hint="Formato YYYY-MM">
              <input style={s.input}
                value={filtros.periodoHasta}
                onChange={e => setFiltros(f => ({ ...f, periodoHasta: e.target.value }))}
                placeholder="2026-06"
                pattern="\d{4}-\d{2}" required />
            </Field>
          </div>

          <div style={{ marginTop: 24 }}>
            <button type="submit" disabled={cargando} style={{
              ...s.btn,
              ...(cargando ? s.btnDisabled : {}),
            }}>
              {cargando ? 'Consultando...' : 'Consultar consolidado'}
            </button>
          </div>
        </form>
      </div>

      {/* Error */}
      {error && (
        <div style={s.alertError}>
          <strong>Error:</strong> {error}
        </div>
      )}

      {/* Resultados */}
      {consolidado && (
        <div style={s.card}>

          {/* Totalizador */}
          <div style={s.totalBox}>
            <div>
              <p style={s.totalLabel}>Total aportado en el periodo</p>
              <p style={s.totalValue}>
                {consolidado.totalAportado?.toLocaleString('es-CO', {
                  style: 'currency', currency: 'COP', maximumFractionDigits: 0,
                })}
              </p>
            </div>
            <div style={s.totalMeta}>
              <span>{consolidado.periodoDesde}</span>
              <span style={{ color: '#B1B1B1', margin: '0 6px' }}>→</span>
              <span>{consolidado.periodoHasta}</span>
              <span style={s.countBadge}>{consolidado.detalle?.length ?? 0} aportes</span>
            </div>
          </div>

          {/* Tabla o empty state */}
          {consolidado.detalle?.length > 0 ? (
            <div style={{ overflowX: 'auto', marginTop: 20 }}>
              <table style={s.table}>
                <thead>
                  <tr>
                    {['Fecha', 'Monto (COP)', 'Canal', 'Periodo', 'Revisión'].map(h => (
                      <th key={h} style={s.th}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {consolidado.detalle.map((a, i) => (
                    <tr key={a.id}
                      style={{ background: i % 2 === 0 ? '#FFFFFF' : '#F9FAFB' }}>
                      <td style={s.td}>{a.fecha}</td>
                      <td style={{ ...s.td, fontWeight: 600 }}>
                        {a.monto?.toLocaleString('es-CO', {
                          style: 'currency', currency: 'COP', maximumFractionDigits: 0,
                        })}
                      </td>
                      <td style={s.td}>
                        <span style={s.canalBadge}>{a.canal}</span>
                      </td>
                      <td style={{ ...s.td, color: '#6B7280' }}>{a.periodo}</td>
                      <td style={s.td}>
                        {a.marcadaRevision
                          ? <span style={s.revisionSi}>⚠ Sí</span>
                          : <span style={s.revisionNo}>✓ No</span>}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div style={s.emptyState}>
              <p style={s.emptyIcon}>◎</p>
              <p style={s.emptyTitle}>Sin aportes en este periodo</p>
              <p style={s.emptyText}>
                No se encontraron aportes para <strong>{consolidado.afiliadoId}</strong> entre{' '}
                {consolidado.periodoDesde} y {consolidado.periodoHasta}.
              </p>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

function Field({ label, hint, children }) {
  return (
    <div>
      <label style={s.label}>{label}</label>
      {hint && <span style={s.hint}>{hint}</span>}
      {children}
    </div>
  )
}

const s = {
  card: {
    background: '#FFFFFF', borderRadius: 10,
    boxShadow: '0 1px 3px rgba(0,0,0,0.08)', padding: 28,
  },
  cardTitle: {
    fontSize: 13, fontWeight: 600,
    color: '#6B7280', textTransform: 'uppercase',
    letterSpacing: 1, marginBottom: 20, marginTop: 0,
  },
  grid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
    gap: 20,
  },
  label: { display: 'block', fontSize: 13, fontWeight: 600, color: '#374151', marginBottom: 2 },
  hint:  { display: 'block', fontSize: 12, color: '#9CA3AF', marginBottom: 6 },
  input: {
    width: '100%', padding: '9px 12px',
    border: '1.5px solid #D1D5DB', borderRadius: 6,
    fontSize: 14, color: '#111827', outline: 'none',
    boxSizing: 'border-box',
  },
  btn: {
    padding: '10px 24px', background: '#6CB531',
    color: '#fff', border: 'none', borderRadius: 6,
    fontSize: 14, fontWeight: 600, cursor: 'pointer',
  },
  btnDisabled: { background: '#B1B1B1', cursor: 'not-allowed' },
  alertError: {
    padding: '12px 16px', background: '#FEF2F2',
    border: '1px solid #FECACA', borderRadius: 6,
    color: '#B91C1C', fontSize: 14,
  },
  totalBox: {
    display: 'flex', justifyContent: 'space-between',
    alignItems: 'flex-start', flexWrap: 'wrap', gap: 12,
    padding: '16px 20px', background: '#F0FDF4',
    borderRadius: 8, border: '1px solid #BBF7D0',
  },
  totalLabel: { fontSize: 12, color: '#6B7280', margin: 0, fontWeight: 500 },
  totalValue: { fontSize: 26, fontWeight: 700, color: '#111827', margin: '4px 0 0' },
  totalMeta: {
    display: 'flex', alignItems: 'center',
    gap: 4, fontSize: 13, color: '#374151', flexWrap: 'wrap',
  },
  countBadge: {
    marginLeft: 12, padding: '2px 10px',
    background: '#94C11F', color: '#fff',
    borderRadius: 20, fontSize: 12, fontWeight: 600,
  },
  table: { width: '100%', borderCollapse: 'collapse', fontSize: 14 },
  th: {
    padding: '10px 14px', textAlign: 'left',
    fontSize: 12, fontWeight: 600, color: '#6B7280',
    textTransform: 'uppercase', letterSpacing: 0.5,
    borderBottom: '2px solid #E5E7EB', background: '#F9FAFB',
  },
  td: { padding: '11px 14px', borderBottom: '1px solid #F3F4F6', color: '#111827' },
  canalBadge: {
    padding: '2px 8px', background: '#EFF6FF',
    color: '#1D4ED8', borderRadius: 4,
    fontSize: 12, fontWeight: 500,
  },
  revisionSi: {
    padding: '2px 8px', background: '#FEF3C7',
    color: '#92400E', borderRadius: 4,
    fontSize: 12, fontWeight: 600,
  },
  revisionNo: {
    padding: '2px 8px', background: '#F0FDF4',
    color: '#166534', borderRadius: 4,
    fontSize: 12, fontWeight: 600,
  },
  emptyState: {
    textAlign: 'center', padding: '48px 24px',
  },
  emptyIcon:  { fontSize: 36, color: '#D1D5DB', margin: '0 0 12px' },
  emptyTitle: { fontSize: 16, fontWeight: 600, color: '#374151', margin: '0 0 8px' },
  emptyText:  { fontSize: 14, color: '#6B7280', margin: 0 },
}
