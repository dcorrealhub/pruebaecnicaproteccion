import { useState } from 'react'
import { registrarAporte } from '../api/aportesApi'

/** Fecha local de hoy en formato yyyy-MM-dd (el input date trabaja en hora local). */
function hoy() {
  const d = new Date()
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/**
 * Formulario de registro de aporte.
 *
 * Idempotencia: la clave se genera una vez por intento de formulario. Se conserva si el envío
 * falla (un reintento no duplica el aporte) y se regenera tras un registro exitoso o cuando
 * cambian los datos (es un aporte distinto).
 */
export default function RegistrarAporte() {
  const [form, setForm] = useState({ afiliadoId: '', monto: '', fecha: hoy(), canal: 'APP_MOVIL' })
  const [idempotenciaKey, setIdempotenciaKey] = useState(() => crypto.randomUUID())
  const [resultado, setResultado] = useState(null)
  const [error, setError] = useState(null)
  const [cargando, setCargando] = useState(false)

  function actualizar(campo, valor) {
    setForm(f => ({ ...f, [campo]: valor }))
    setIdempotenciaKey(crypto.randomUUID())
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    setResultado(null)

    if (!(Number(form.monto) > 0)) {
      setError('El monto debe ser mayor a cero')
      return
    }

    setCargando(true)
    try {
      const data = await registrarAporte({ ...form, idempotenciaKey })
      setResultado(data)
      setIdempotenciaKey(crypto.randomUUID())
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
            maxLength={50}
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
          Fecha del aporte
          <input
            type="date"
            value={form.fecha}
            max={hoy()}
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
        <p style={{ color: 'red', marginTop: 16 }}>Error: {error}</p>
      )}

      {resultado && (
        <div style={{ marginTop: 16, padding: 12, background: '#f0f0f0' }}>
          <p>
            {resultado.repetido
              ? `Este aporte ya estaba registrado (reintento). ID: ${resultado.aporte.id}`
              : `Aporte registrado. ID: ${resultado.aporte.id}`}
          </p>
          {resultado.aporte.marcadaRevision && (
            <p style={{ color: 'orange' }}>Este aporte quedó marcado para revisión.</p>
          )}
        </div>
      )}
    </div>
  )
}
