import { useState } from 'react'
import RegistrarAporte from './components/RegistrarAporte'
import ConsolidadoAportes from './components/ConsolidadoAportes'

export default function App() {
  const [vistaActiva, setVistaActiva] = useState('registrar')

  return (
    <div style={{ fontFamily: 'sans-serif', maxWidth: 800, margin: '0 auto', padding: 24 }}>
      <h1 style={{ fontSize: 22, marginBottom: 24 }}>Aportes Voluntarios</h1>

      <nav style={{ marginBottom: 24, display: 'flex', gap: 12 }}>
        <button
          onClick={() => setVistaActiva('registrar')}
          style={{ fontWeight: vistaActiva === 'registrar' ? 'bold' : 'normal' }}
        >
          Registrar aporte
        </button>
        <button
          onClick={() => setVistaActiva('consolidado')}
          style={{ fontWeight: vistaActiva === 'consolidado' ? 'bold' : 'normal' }}
        >
          Consolidado
        </button>
      </nav>

      {vistaActiva === 'registrar' && <RegistrarAporte />}
      {vistaActiva === 'consolidado' && <ConsolidadoAportes />}
    </div>
  )
}
