import { useState } from 'react'
import AporteForm from './components/AporteForm'
import Consolidado from './components/Consolidado'
import './App.css'

const API_BASE = 'http://localhost:8080'

function App() {
  const [refresh, setRefresh] = useState(0)

  return (
    <div className="app">
      <h1>Registro de Aportes Voluntarios</h1>
      <AporteForm apiBase={API_BASE} onRegistered={() => setRefresh(r => r + 1)} />
      <Consolidado apiBase={API_BASE} refresh={refresh} />
    </div>
  )
}

export default App