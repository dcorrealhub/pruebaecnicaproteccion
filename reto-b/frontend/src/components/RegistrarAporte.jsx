import { useState } from 'react'
import { registrarAporte } from '../api/aportesApi'

export default function RegistrarAporte() {
  const [form, setForm] = useState({ afiliadoId: '', monto: '', canal: 'APP_MOVIL' })
  const [resultado, setResultado] = useState(null)
  const [error, setError] = useState(null)
  const [cargando, setCargando] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    setResultado(null)
    setCargando(true)

    try {
      const data = await registrarAporte({
        ...form,
        monto: parseFloat(form.monto),
        idempotenciaKey: crypto.randomUUID(),
      })
      setResultado(data)
    } catch (err) {
      setError(err.message)
    } finally {
      setCargando(false)
    }
  }

  return (
    <div>
      <h2>Registrar aporte</h2>

      <form onSubmit={handleSubmit} className="form-stack">
        <div className="field">
          <label>ID Afiliado</label>
          <input
            value={form.afiliadoId}
            onChange={e => setForm(f => ({ ...f, afiliadoId: e.target.value.toUpperCase() }))}
            placeholder="AF-001"
            required
          />
        </div>

        <div className="field">
          <label>Monto (COP)</label>
          <input
            type="number"
            min="0.01"
            step="0.01"
            value={form.monto}
            onChange={e => setForm(f => ({ ...f, monto: e.target.value }))}
            placeholder="0.00"
            required
          />
        </div>

        <div className="field">
          <label>Canal</label>
          <select
            value={form.canal}
            onChange={e => setForm(f => ({ ...f, canal: e.target.value }))}
          >
            <option value="APP_MOVIL">App móvil</option>
            <option value="WEB">Web</option>
            <option value="SUCURSAL">Sucursal</option>
          </select>
        </div>

        <button type="submit" className="btn-primary" disabled={cargando}>
          {cargando ? 'Registrando...' : 'Registrar aporte'}
        </button>
      </form>

      {error && (
        <div className="alert alert-error">
          <span>✕</span>
          <span>{error}</span>
        </div>
      )}

      {resultado && (
        <div className="alert alert-success">
          <span>✓</span>
          <div>
            <strong>Aporte registrado.</strong> ID: {resultado.id}
            {resultado.marcadaRevision && (
              <div className="alert alert-warning" style={{ marginTop: 10 }}>
                <span>⚠</span>
                <span>Este aporte quedó marcado para revisión.</span>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
