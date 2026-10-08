import { useState } from 'react'
import RegistrarAporte from './components/RegistrarAporte'
import ConsolidadoAportes from './components/ConsolidadoAportes'

export default function App() {
  // Último aporte registrado; seq cambia en cada registro para disparar el refresco
  // del consolidado aunque se registre dos veces seguidas para el mismo afiliado.
  const [ultimoRegistro, setUltimoRegistro] = useState(null)

  function handleRegistrado(aporte) {
    setUltimoRegistro(prev => ({ afiliadoId: aporte.afiliadoId, seq: (prev?.seq ?? 0) + 1 }))
  }

  return (
    <>
      <header className="header">
        <div className="header__inner">
          <h1>Aportes Voluntarios</h1>
          <p>Registro y consulta del consolidado mensual por afiliado</p>
        </div>
      </header>

      <main className="main">
        <RegistrarAporte onRegistrado={handleRegistrado} />
        <ConsolidadoAportes ultimoRegistro={ultimoRegistro} />
      </main>
    </>
  )
}
