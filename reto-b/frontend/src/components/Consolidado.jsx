import { useState, useEffect } from 'react'

const API_BASE = 'http://localhost:8080'

function Consolidado({ apiBase, refresh }) {
  const [idAfiliado, setIdAfiliado] = useState('AF-001')
  const [periodoDesde, setPeriodoDesde] = useState('2025-06')
  const [periodoHasta, setPeriodoHasta] = useState('2025-07')
  const [consolidado, setConsolidado] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    cargarConsolidado()
  }, [refresh])

  const cargarConsolidado = async () => {
    setLoading(true)
    setError('')
    try {
      const params = new URLSearchParams({
        idAfiliado,
        periodoDesde,
        periodoHasta
      })

      const response = await fetch(`${API_BASE}/api/aportes/consolidado?${params}`)
      if (!response.ok) {
        const errorText = await response.text()
        throw new Error(errorText || 'Error al consultar consolidado')
      }
      const data = await response.json()
      setConsolidado(data)
    } catch (err) {
      setError(err.message || 'Error al consultar')
    } finally {
      setLoading(false)
    }
  }

  const onSubmit = async (e) => {
    e.preventDefault()
    await cargarConsolidado()
  }

  return (
    <div className="consolidado">
      <h2>Consolidado de Aportes</h2>

      <form className="form" onSubmit={onSubmit}>
        <label>
          ID Afiliado
          <input value={idAfiliado} onChange={(e) => setIdAfiliado(e.target.value)} />
        </label>

        <label>
          Periodo Desde
          <input value={periodoDesde} onChange={(e) => setPeriodoDesde(e.target.value)} placeholder="YYYY-MM" />
        </label>

        <label>
          Periodo Hasta
          <input value={periodoHasta} onChange={(e) => setPeriodoHasta(e.target.value)} placeholder="YYYY-MM" />
        </label>

        <button type="submit">Consultar</button>
      </form>

      {error && <p className="error">{error}</p>}

      {loading && <p>Cargando...</p>}

      {consolidado && (
        <div className="resultado">
          <p>
            <strong>Total aportado:</strong> {consolidado.totalAportado}
          </p>
          <table className="tabla">
            <thead>
              <tr>
                <th>Id</th>
                <th>Fecha</th>
                <th>Canal</th>
                <th>Monto</th>
                <th>Periodo</th>
                <th>Revision</th>
              </tr>
            </thead>
            <tbody>
              {consolidado.detalle.map((aporte) => (
                <tr key={aporte.id}>
                  <td>{aporte.id}</td>
                  <td>{aporte.fecha}</td>
                  <td>{aporte.canal}</td>
                  <td>{aporte.monto}</td>
                  <td>{aporte.periodo}</td>
                  <td>{aporte.marcadaRevision ? 'Si' : 'No'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

export default Consolidado