import { useState } from 'react'
import RegistrarAporte from './components/RegistrarAporte'
import ConsolidadoAportes from './components/ConsolidadoAportes'

const NAV_ITEMS = [
  { id: 'registrar', label: 'Registrar aporte', icon: '＋' },
  { id: 'consolidado', label: 'Consolidado', icon: '◎' },
]

export default function App() {
  const [vista, setVista] = useState('registrar')

  return (
    <div style={styles.root}>
      {/* Sidebar */}
      <aside style={styles.sidebar}>
        <div style={styles.brand}>
          <div style={styles.brandDot} />
          <span style={styles.brandText}>PROTEX</span>
        </div>
        <p style={styles.brandSub}>Aportes Voluntarios</p>
        <nav style={{ marginTop: 32 }}>
          {NAV_ITEMS.map(item => (
            <button
              key={item.id}
              onClick={() => setVista(item.id)}
              style={{
                ...styles.navItem,
                ...(vista === item.id ? styles.navItemActive : {}),
              }}
            >
              <span style={styles.navIcon}>{item.icon}</span>
              {item.label}
            </button>
          ))}
        </nav>
      </aside>

      {/* Main */}
      <main style={styles.main}>
        <header style={styles.header}>
          <div>
            <h1 style={styles.pageTitle}>
              {NAV_ITEMS.find(i => i.id === vista)?.label}
            </h1>
            <p style={styles.pageSubtitle}>
              {vista === 'registrar'
                ? 'Registrá un aporte a un fondo voluntario'
                : 'Consultá el consolidado por afiliado y periodo'}
            </p>
          </div>
        </header>

        <div style={styles.content}>
          {vista === 'registrar' && <RegistrarAporte />}
          {vista === 'consolidado' && <ConsolidadoAportes />}
        </div>
      </main>
    </div>
  )
}

const styles = {
  root: {
    display: 'flex', minHeight: '100vh',
    fontFamily: "'Inter', system-ui, sans-serif",
    background: '#F7F9F5', color: '#111827',
  },
  sidebar: {
    width: 240, background: '#111827', padding: '28px 16px',
    display: 'flex', flexDirection: 'column', flexShrink: 0,
  },
  brand: { display: 'flex', alignItems: 'center', gap: 10, paddingLeft: 8 },
  brandDot: {
    width: 28, height: 28, borderRadius: 6,
    background: '#94C11F', flexShrink: 0,
  },
  brandText: {
    color: '#FFFFFF', fontSize: 18,
    fontWeight: 700, letterSpacing: 2,
  },
  brandSub: {
    color: '#6B7280', fontSize: 12,
    marginTop: 6, paddingLeft: 8,
  },
  navItem: {
    display: 'flex', alignItems: 'center', gap: 10,
    width: '100%', padding: '10px 12px', marginBottom: 4,
    background: 'transparent', border: 'none',
    borderRadius: 6, color: '#9CA3AF',
    fontSize: 14, fontWeight: 500,
    cursor: 'pointer', textAlign: 'left',
    transition: 'all 0.15s',
  },
  navItemActive: {
    background: 'rgba(148,193,31,0.12)',
    color: '#94C11F',
    borderLeft: '3px solid #94C11F',
    paddingLeft: 9,
  },
  navIcon: { fontSize: 16, width: 20, textAlign: 'center' },
  header: {
    padding: '28px 32px 0',
    borderBottom: '1px solid #E5E7EB',
    paddingBottom: 20, background: '#FFFFFF',
  },
  pageTitle: { fontSize: 22, fontWeight: 700, margin: 0, color: '#111827' },
  pageSubtitle: { fontSize: 14, color: '#6B7280', marginTop: 4 },
  main: { flex: 1, display: 'flex', flexDirection: 'column', overflow: 'auto' },
  content: { padding: 32, flex: 1 },
}
