import { useState } from 'react'
import { registrarAporte, AporteResponse } from '../api/aportesApi'

interface FormState {
  afiliadoId: string
  monto: string
  canal: string
}

export default function RegistrarAporte() {
  const [form, setForm] = useState<FormState>({ afiliadoId: '', monto: '1000000', canal: 'APP_MOVIL' })
  const [idempotenciaKey, setIdempotenciaKey] = useState<string>(() => crypto.randomUUID())
  const [resultado, setResultado] = useState<AporteResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [cargando, setCargando] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setResultado(null)
    setCargando(true)

    try {
      const data = await registrarAporte({
        ...form,
        monto: parseFloat(form.monto),
        idempotenciaKey,
      })
      setResultado(data)
      setIdempotenciaKey(crypto.randomUUID())
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error inesperado')
    } finally {
      setCargando(false)
    }
  }

  return (
    <div>
      <h2 className="text-lg font-semibold text-gray-700 mb-6">Registrar aporte</h2>

      <form onSubmit={handleSubmit} className="flex flex-col gap-4 max-w-md">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            ID Afiliado (sintético)
          </label>
          <input
            value={form.afiliadoId}
            onChange={e => setForm(f => ({ ...f, afiliadoId: e.target.value }))}
            placeholder="AF-001"
            required
            className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Monto (COP)</label>
          <input
            type="number"
            min="100000"
            step="100000"
            value={form.monto}
            onChange={e => setForm(f => ({ ...f, monto: e.target.value }))}
            required
            className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Canal</label>
          <select
            value={form.canal}
            onChange={e => setForm(f => ({ ...f, canal: e.target.value }))}
            className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="APP_MOVIL">App móvil</option>
            <option value="WEB">Web</option>
            <option value="SUCURSAL">Sucursal</option>
          </select>
        </div>

        <button
          type="submit"
          disabled={cargando}
          className="bg-blue-600 text-white rounded-md px-4 py-2 text-sm font-medium hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          {cargando ? 'Registrando...' : 'Registrar'}
        </button>
      </form>

      {error && (
        <div className="mt-4 p-3 bg-red-50 border border-red-200 rounded-md">
          <p className="text-sm text-red-700">{error}</p>
        </div>
      )}

      {resultado && (
        <div className="mt-4 p-4 bg-green-50 border border-green-200 rounded-md">
          <p className="text-sm text-green-800 font-medium">Aporte registrado — ID: {resultado.id}</p>
          {resultado.marcadaRevision && (
            <p className="text-sm text-amber-700 mt-1">
              Este aporte quedó marcado para revisión manual.
            </p>
          )}
        </div>
      )}
    </div>
  )
}
