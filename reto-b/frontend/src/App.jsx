import { useState } from 'react'
import RegistrarAporte from './components/RegistrarAporte'
import ConsolidadoAportes from './components/ConsolidadoAportes'
import GestionAportes from './components/GestionAportes'

export default function App() {
  const [vistaActiva, setVistaActiva] = useState('registrar')

  return (
    <div className="app-container">
      {/* Premium Header */}
      <header className="app-header">
        <div className="brand-section">
          <div className="brand-logo">P</div>
          <div>
            <h1 className="brand-title" style={{ fontSize: '1.4rem', lineHeight: '1.2' }}>PROTECCIÓN</h1>
            <span className="brand-subtitle">Gestión de Aportes Voluntarios</span>
          </div>
        </div>
        
        {/* Navigation */}
        <nav className="nav-container">
          <button
            onClick={() => setVistaActiva('registrar')}
            className={`nav-btn ${vistaActiva === 'registrar' ? 'active' : ''}`}
          >
            Registrar Aporte
          </button>
          <button
            onClick={() => setVistaActiva('consolidado')}
            className={`nav-btn ${vistaActiva === 'consolidado' ? 'active' : ''}`}
          >
            Consolidado de Aportes
          </button>
          <button
            onClick={() => setVistaActiva('gestion')}
            className={`nav-btn ${vistaActiva === 'gestion' ? 'active' : ''}`}
          >
            Gestión de Revisiones
          </button>
        </nav>
      </header>

      {/* Main Grid Layout */}
      <main className="main-content">
        {/* Side Panel for Stats / Context */}
        <aside className="stats-panel fade-in">
          <div className="card">
            <h3 style={{ fontSize: '1.1rem', marginBottom: '1rem', borderBottom: '1px solid var(--border-light)', paddingBottom: '0.5rem' }}>
              Resumen Operativo
            </h3>
            
            <div className="stat-card" style={{ marginBottom: '0.75rem' }}>
              <div className="stat-icon">💰</div>
              <div className="stat-info">
                <span className="stat-label">Monto Mínimo</span>
                <span className="stat-value">$ 0.01</span>
              </div>
            </div>

            <div className="stat-card" style={{ marginBottom: '0.75rem' }}>
              <div className="stat-icon">⚡</div>
              <div className="stat-info">
                <span className="stat-label">Canales Habilitados</span>
                <span className="stat-value">3 Activos</span>
              </div>
            </div>

            <div className="stat-card">
              <div className="stat-icon">🔒</div>
              <div className="stat-info">
                <span className="stat-label">Idempotencia</span>
                <span className="stat-value" style={{ fontSize: '0.9rem', color: 'var(--color-success)', fontWeight: '600' }}>Activa (UUID)</span>
              </div>
            </div>
          </div>

          <div className="card" style={{ padding: '1.25rem' }}>
            <h4 style={{ fontSize: '0.9rem', color: 'var(--text-main)', marginBottom: '0.5rem' }}>💡 Consejos rápidos</h4>
            <ul style={{ paddingLeft: '1.2rem', fontSize: '0.8rem', color: 'var(--text-muted)', lineHeight: '1.4' }}>
              <li style={{ marginBottom: '0.4rem' }}>Todos los aportes se guardan con una llave de idempotencia única para prevenir duplicados.</li>
              <li>Aportes mayores a la cantidad permitida serán redirigidos a revisión manual por el sistema de control.</li>
            </ul>
          </div>
        </aside>

        {/* Dynamic Workspace Container */}
        <section className="fade-in">
          {vistaActiva === 'registrar'  && <RegistrarAporte />}
          {vistaActiva === 'consolidado' && <ConsolidadoAportes />}
          {vistaActiva === 'gestion'     && <GestionAportes />}
        </section>
      </main>
    </div>
  )
}

