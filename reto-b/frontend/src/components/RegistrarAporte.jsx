import { useState } from 'react'
import { registrarAporte } from '../api/aportesApi'

const HOY = new Date().toISOString().slice(0, 10) // YYYY-MM-DD

const ESTADO_INICIAL = { afiliadoId: '', monto: '', fecha: HOY, canal: 'APP_MOVIL' }

/**
 * Formulario de registro de aporte.
 *
 * - La idempotenciaKey se genera en el cliente (dentro de aportesApi) con crypto.randomUUID().
 * - El monto se envía como string para preservar precisión (ver aportesApi).
 * - La fecha no puede ser futura (el backend también lo valida).
 */
export default function RegistrarAporte() {
  const [form, setForm] = useState(ESTADO_INICIAL)
  const [resultado, setResultado] = useState(null)
  const [error, setError] = useState(null)
  const [cargando, setCargando] = useState(false)

  function actualizar(campo, valor) {
    setForm(f => ({ ...f, [campo]: valor }))
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    setResultado(null)

    const monto = Number(form.monto)
    if (!(monto > 0)) {
      setError('El monto debe ser mayor a cero.')
      return
    }
    if (form.fecha > HOY) {
      setError('La fecha del aporte no puede ser futura.')
      return
    }

    setCargando(true)
    try {
      const data = await registrarAporte({
        afiliadoId: form.afiliadoId.trim(),
        monto: form.monto,
        fecha: form.fecha,
        canal: form.canal,
      })
      setResultado(data)
      setForm(ESTADO_INICIAL)
    } catch (err) {
      setError(err.message)
    } finally {
      setCargando(false)
    }
  }

  return (
    <div>
      <h2 style={{ fontSize: 18, marginBottom: 16 }}>Registrar aporte</h2>

      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 12, maxWidth: 400 }}>
        <label>
          ID Afiliado (sintético)
          <input
            value={form.afiliadoId}
            onChange={e => actualizar('afiliadoId', e.target.value)}
            placeholder="AF-001"
            required
            style={{ display: 'block', width: '100%', marginTop: 4 }}
          />
        </label>

        <label>
          Monto (COP)
          <input
            type="number"
            min="0.01"
            step="0.01"
            value={form.monto}
            onChange={e => actualizar('monto', e.target.value)}
            required
            style={{ display: 'block', width: '100%', marginTop: 4 }}
          />
        </label>

        <label>
          Fecha
          <input
            type="date"
            max={HOY}
            value={form.fecha}
            onChange={e => actualizar('fecha', e.target.value)}
            required
            style={{ display: 'block', width: '100%', marginTop: 4 }}
          />
        </label>

        <label>
          Canal
          <select
            value={form.canal}
            onChange={e => actualizar('canal', e.target.value)}
            style={{ display: 'block', width: '100%', marginTop: 4 }}
          >
            <option value="APP_MOVIL">App móvil</option>
            <option value="WEB">Web</option>
            <option value="SUCURSAL">Sucursal</option>
          </select>
        </label>

        <button type="submit" disabled={cargando}>
          {cargando ? 'Registrando...' : 'Registrar'}
        </button>
      </form>

      {error && (
        <p style={{ color: 'red', marginTop: 16 }} role="alert">Error: {error}</p>
      )}

      {resultado && (
        <div style={{ marginTop: 16, padding: 12, background: '#f0f0f0' }}>
          <p>Aporte registrado. ID: {resultado.id}</p>
          {resultado.marcadaRevision && (
            <p style={{ color: '#b36b00' }}>Este aporte quedó marcado para revisión.</p>
          )}
        </div>
      )}
    </div>
  )
}
