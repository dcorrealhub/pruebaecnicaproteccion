import { useState } from 'react'
import RegistrarAporte from './components/RegistrarAporte'
import ConsolidadoAportes from './components/ConsolidadoAportes'
import Configuracion from './components/Configuracion'

const VISTAS = [
  { id: 'registrar', label: 'Registrar aporte' },
  { id: 'consolidado', label: 'Consolidado' },
  { id: 'configuracion', label: 'Configuración' },
]

export default function App() {
  const [vistaActiva, setVistaActiva] = useState('registrar')

  return (
    <div style={{ fontFamily: 'sans-serif', maxWidth: 800, margin: '0 auto', padding: 24 }}>
      <h1 style={{ fontSize: 22, marginBottom: 24 }}>Aportes Voluntarios</h1>

      <nav style={{ marginBottom: 24, display: 'flex', gap: 12 }}>
        {VISTAS.map(v => (
          <button
            key={v.id}
            onClick={() => setVistaActiva(v.id)}
            style={{ fontWeight: vistaActiva === v.id ? 'bold' : 'normal' }}
          >
            {v.label}
          </button>
        ))}
      </nav>

      {vistaActiva === 'registrar' && <RegistrarAporte />}
      {vistaActiva === 'consolidado' && <ConsolidadoAportes />}
      {vistaActiva === 'configuracion' && <Configuracion />}
    </div>
  )
}
