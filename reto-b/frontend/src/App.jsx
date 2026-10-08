import { useState } from 'react'
import RegistrarAporte from './components/RegistrarAporte'
import ConsolidadoAportes from './components/ConsolidadoAportes'

const VISTAS = [
  { id: 'registrar', etiqueta: 'Registrar aporte' },
  { id: 'consolidado', etiqueta: 'Consolidado' },
]

export default function App() {
  const [vistaActiva, setVistaActiva] = useState('registrar')

  return (
    <>
      <header className="app-header">
        <div className="app-header__inner">
          <div className="brand">
            <div className="brand__logo" aria-hidden="true">A</div>
            <div>
              <h1 className="brand__title">Aportes Voluntarios</h1>
              <p className="brand__subtitle">Fondo voluntario · entorno de prueba con datos sintéticos</p>
            </div>
          </div>

          <nav className="tabs" role="tablist" aria-label="Secciones">
            {VISTAS.map(v => (
              <button
                key={v.id}
                role="tab"
                className="tabs__btn"
                aria-selected={vistaActiva === v.id}
                onClick={() => setVistaActiva(v.id)}
              >
                {v.etiqueta}
              </button>
            ))}
          </nav>
        </div>
      </header>

      <main className="app-main">
        {vistaActiva === 'registrar' && <RegistrarAporte />}
        {vistaActiva === 'consolidado' && <ConsolidadoAportes />}
      </main>
    </>
  )
}
