import { useState } from 'react'
import Login from './components/Login'
import RegistrarAporte from './components/RegistrarAporte'
import ConsolidadoAportes from './components/ConsolidadoAportes'
import { setToken } from './api/aportesApi'

export default function App() {
  const [autenticado, setAutenticado] = useState(false)
  const [vistaActiva, setVistaActiva] = useState('registrar')

  function handleLogin(token: string) {
    setToken(token)
    setAutenticado(true)
  }

  if (!autenticado) {
    return <Login onLogin={handleLogin} />
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white border-b border-gray-200 px-6 py-4">
        <h1 className="text-xl font-semibold text-gray-800">Aportes Voluntarios</h1>
      </header>

      <div className="max-w-3xl mx-auto px-6 py-8">
        <nav className="flex gap-2 mb-8 border-b border-gray-200">
          <button
            onClick={() => setVistaActiva('registrar')}
            className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
              vistaActiva === 'registrar'
                ? 'border-blue-600 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            Registrar aporte
          </button>
          <button
            onClick={() => setVistaActiva('consolidado')}
            className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
              vistaActiva === 'consolidado'
                ? 'border-blue-600 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            Consolidado
          </button>
        </nav>

        {vistaActiva === 'registrar' && <RegistrarAporte />}
        {vistaActiva === 'consolidado' && <ConsolidadoAportes />}
      </div>
    </div>
  )
}
