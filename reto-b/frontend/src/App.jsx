import { useState } from 'react'
import RegistrarAporte from './components/RegistrarAporte'
import ConsolidadoAportes from './components/ConsolidadoAportes'
import './styles.css'

export default function App() {
  const [vistaActiva, setVistaActiva] = useState('registrar')

  return (
    <div className="app-shell">
      <header className="app-header">
        <div className="app-header__logo">P</div>
        <div>
          <div className="app-header__title">Aportes Voluntarios</div>
          <div className="app-header__subtitle">CIS Proteccion S.A. — Sistema de fondos voluntarios</div>
        </div>
      </header>

      <nav className="nav-tabs">
        <button
          className={`nav-tab ${vistaActiva === 'registrar' ? 'nav-tab--active' : ''}`}
          onClick={() => setVistaActiva('registrar')}
        >
          Registrar aporte
        </button>
        <button
          className={`nav-tab ${vistaActiva === 'consolidado' ? 'nav-tab--active' : ''}`}
          onClick={() => setVistaActiva('consolidado')}
        >
          Consolidado
        </button>
      </nav>

      {vistaActiva === 'registrar' && <RegistrarAporte />}
      {vistaActiva === 'consolidado' && <ConsolidadoAportes />}
    </div>
  )
}
