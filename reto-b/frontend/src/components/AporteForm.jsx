import { useState } from 'react'

const API_BASE = 'http://localhost:8080'

function AporteForm({ apiBase, onRegistered }) {
  const [idAfiliado, setIdAfiliado] = useState('AF-001')
  const [monto, setMonto] = useState('')
  const [canal, setCanal] = useState('WEB')
  const [idempotenciaKey, setIdempotenciaKey] = useState(`A-${Date.now()}`)
  const [error, setError] = useState('')
  const [exito, setExito] = useState('')
  const [cargando, setCargando] = useState(false)

  const generarKey = () => {
    setIdempotenciaKey(`A-${Date.now()}`)
  }

  const onSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setExito('')
    setCargando(true)

    try {
      const response = await fetch(`${apiBase}/api/aportes`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          idAfiliado,
          monto: parseFloat(monto),
          canal,
          idempotenciaKey
        })
      })

      if (!response.ok) {
        const body = await response.text()
        throw new Error(body || 'Error al registrar el aporte')
      }

      const data = await response.json()
      setExito(`Aporte registrado correctamente. id=${data.id}, monto=${data.monto}`)
      setMonto('')
      generarKey()
      onRegistered()
    } catch (err) {
      setError(err.message || 'Error al registrar el aporte')
    } finally {
      setCargando(false)
    }
  }

  return (
    <form className="form" onSubmit={onSubmit}>
      <h2>Registrar Aporte</h2>

      <label>
        ID Afiliado
        <input value={idAfiliado} onChange={(e) => setIdAfiliado(e.target.value)} />
      </label>

      <label>
        Monto
        <input
          type="number"
          step="0.01"
          min="0.0001"
          value={monto}
          onChange={(e) => setMonto(e.target.value)}
          required
        />
      </label>

      <label>
        Canal
        <input value={canal} onChange={(e) => setCanal(e.target.value)} />
      </label>

      <label>
        Clave de idempotencia
        <input
          value={idempotenciaKey}
          onChange={(e) => setIdempotenciaKey(e.target.value)}
          required
        />
      </label>

      <div className="actions">
        <button type="submit" disabled={cargando}>
          {cargando ? 'Registrando...' : 'Registrar aporte'}
        </button>
        <button type="button" onClick={generarKey}>Nueva clave</button>
      </div>

      {error && <p className="error">{error}</p>}
      {exito && <p className="success">{exito}</p>}
    </form>
  )
}

export default AporteForm