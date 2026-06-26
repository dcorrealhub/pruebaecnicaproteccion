import { useState } from 'react'
import RegistrarAporte from './components/RegistrarAporte'
import ConsolidadoAportes from './components/ConsolidadoAportes'

export default function App() {
  const [vistaActiva, setVistaActiva] = useState('registrar')

  return (
    <div className="app-wrapper">
      <header className="app-header">
        <h1>Aportes Voluntarios</h1>
      </header>

      <nav className="tabs">
        <button
          className={`tab-btn${vistaActiva === 'registrar' ? ' active' : ''}`}
          onClick={() => setVistaActiva('registrar')}
        >
          Registrar aporte
        </button>
        <button
          className={`tab-btn${vistaActiva === 'consolidado' ? ' active' : ''}`}
          onClick={() => setVistaActiva('consolidado')}
        >
          Consolidado
        </button>
      </nav>

      <div className="card">
        {vistaActiva === 'registrar' && <RegistrarAporte />}
        {vistaActiva === 'consolidado' && <ConsolidadoAportes />}
      </div>
    </div>
  )
}
