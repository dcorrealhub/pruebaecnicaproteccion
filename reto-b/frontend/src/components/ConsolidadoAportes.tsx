import { useState } from 'react'
import { consultarConsolidado } from '../api/aportesApi'
import type { ConsolidadoResponse } from '../types/aportes'

interface Filtros {
  afiliadoId: string
  periodoDesde: string
  periodoHasta: string
}

export default function ConsolidadoAportes() {
  const ahora = new Date()
  const mesActual = ahora.toISOString().slice(0, 7)
  const mesAnterior = new Date(ahora.getFullYear(), ahora.getMonth() - 1, 1).toISOString().slice(0, 7)
  const [filtros, setFiltros] = useState<Filtros>({ afiliadoId: '', periodoDesde: mesAnterior, periodoHasta: mesActual })

  const [consolidado, setConsolidado] = useState<ConsolidadoResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [cargando, setCargando] = useState(false)

  async function handleBuscar(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setConsolidado(null)
    setCargando(true)

    try {
      const data = await consultarConsolidado(filtros)
      setConsolidado(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error inesperado')
    } finally {
      setCargando(false)
    }
  }

  return (
    <div>
      <h2 className="text-lg font-semibold text-gray-700 mb-6">Consolidado de aportes</h2>

      <form onSubmit={handleBuscar} className="flex flex-wrap gap-4 items-end mb-8">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">ID Afiliado</label>
          <input
            value={filtros.afiliadoId}
            onChange={e => setFiltros(f => ({ ...f, afiliadoId: e.target.value }))}
            placeholder="AF-001"
            required
            className="border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Desde</label>
          <input
            type="month"
            value={filtros.periodoDesde}
            max={mesActual}
            onChange={e => setFiltros(f => ({ ...f, periodoDesde: e.target.value }))}
            required
            className="border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Hasta</label>
          <input
            type="month"
            value={filtros.periodoHasta}
            max={mesActual}
            onChange={e => setFiltros(f => ({ ...f, periodoHasta: e.target.value }))}
            required
            className="border border-gray-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        <button
          type="submit"
          disabled={cargando}
          className="bg-blue-600 text-white rounded-md px-4 py-2 text-sm font-medium hover:bg-blue-700 disabled:opacity-50 transition-colors"
        >
          {cargando ? 'Consultando...' : 'Consultar'}
        </button>
      </form>

      {error && (
        <div className="p-3 bg-red-50 border border-red-200 rounded-md mb-4">
          <p className="text-sm text-red-700">{error}</p>
        </div>
      )}

      {consolidado && (
        <div>
          <p className="text-sm text-gray-600 mb-4">
            <span className="font-medium">Total aportado: </span>
            {consolidado.totalAportado?.toLocaleString('es-CO', { style: 'currency', currency: 'COP' })}
          </p>

          {consolidado.detalle?.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="w-full text-sm border-collapse">
                <thead>
                  <tr className="bg-gray-100 text-left text-gray-600">
                    <th className="px-4 py-2 font-medium">Fecha</th>
                    <th className="px-4 py-2 font-medium">Monto</th>
                    <th className="px-4 py-2 font-medium">Canal</th>
                    <th className="px-4 py-2 font-medium">Revisión</th>
                  </tr>
                </thead>
                <tbody>
                  {consolidado.detalle.map(a => (
                    <tr key={a.id} className="border-t border-gray-200 hover:bg-gray-50">
                      <td className="px-4 py-2">{a.fecha}</td>
                      <td className="px-4 py-2">{a.monto?.toLocaleString('es-CO')}</td>
                      <td className="px-4 py-2">{a.canal}</td>
                      <td className="px-4 py-2">
                        {a.marcadaRevision
                          ? <span className="text-amber-600 font-medium">Sí</span>
                          : <span className="text-gray-400">No</span>}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <p className="text-sm text-gray-500">No se encontraron aportes en el periodo indicado.</p>
          )}
        </div>
      )}
    </div>
  )
}
