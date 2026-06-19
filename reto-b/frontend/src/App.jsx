import { useState } from 'react'
import RegistrarAporte from './components/RegistrarAporte'
import ConsolidadoAportes from './components/ConsolidadoAportes'

export default function App() {
  const [vistaActiva, setVistaActiva] = useState('registrar')

  return (
    <div className="page-wrapper">
      <h1 className="page-title">Aportes Voluntarios</h1>
      <p className="page-subtitle">Registro y consulta de aportes al fondo de inversión voluntaria</p>

      <nav className="tab-nav" role="tablist">
        <button
          role="tab"
          aria-selected={vistaActiva === 'registrar'}
          className={`tab-btn${vistaActiva === 'registrar' ? ' active' : ''}`}
          onClick={() => setVistaActiva('registrar')}
        >
          Registrar aporte
        </button>
        <button
          role="tab"
          aria-selected={vistaActiva === 'consolidado'}
          className={`tab-btn${vistaActiva === 'consolidado' ? ' active' : ''}`}
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
