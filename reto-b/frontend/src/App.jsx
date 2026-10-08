import { useState } from 'react'
import RegistrarAporte from './components/RegistrarAporte'
import ConsolidadoAportes from './components/ConsolidadoAportes'
import logoProteccion from './assets/logo_proteccion.png'

export default function App() {
  const [vistaActiva, setVistaActiva] = useState('registrar')

  return (
    <>
      <header className="app-header">
        <div className="app-header__inner">
          <img src={logoProteccion} alt="Protección" className="app-header__logo-img" />
          <span className="app-header__sub">Aportes Voluntarios · CIS</span>
        </div>
      </header>

      <main className="contenedor">
        <h1 className="titulo-pagina">Aportes Voluntarios</h1>

        <nav className="tabs" role="tablist">
          <button
            role="tab"
            aria-selected={vistaActiva === 'registrar'}
            className={`tab ${vistaActiva === 'registrar' ? 'tab--activa' : ''}`}
            onClick={() => setVistaActiva('registrar')}
          >
            Registrar aporte
          </button>
          <button
            role="tab"
            aria-selected={vistaActiva === 'consolidado'}
            className={`tab ${vistaActiva === 'consolidado' ? 'tab--activa' : ''}`}
            onClick={() => setVistaActiva('consolidado')}
          >
            Consolidado
          </button>
        </nav>

        {vistaActiva === 'registrar' && <RegistrarAporte />}
        {vistaActiva === 'consolidado' && <ConsolidadoAportes />}
      </main>
    </>
  )
}
