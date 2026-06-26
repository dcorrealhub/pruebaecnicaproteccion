import { useRef, useState } from 'react'
import { registrarAporte } from '../api/aportesApi'

const CANALES = ['APP_MOVIL', 'WEB', 'SUCURSAL']

/**
 * Formulario de registro de aporte.
 *
 * La idempotenciaKey se genera con crypto.randomUUID() y se conserva mientras el
 * intento no tenga éxito: así un doble clic (o un reintento tras error de red) no
 * crea aportes duplicados. Se regenera solo tras un registro exitoso.
 */
export default function RegistrarAporte() {
  const [form, setForm] = useState({ afiliadoId: '', monto: '', fecha: '', canal: 'APP_MOVIL' })
  const [resultado, setResultado] = useState(null)
  const [error, setError] = useState(null)
  const [cargando, setCargando] = useState(false)
  const idempotenciaKey = useRef(null)

  function actualizar(campo, valor) {
    setForm(f => ({ ...f, [campo]: valor }))
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    setResultado(null)

    const monto = parseFloat(form.monto)
    if (!Number.isFinite(monto) || monto <= 0) {
      setError('El monto debe ser mayor a cero')
      return
    }

    // Reutiliza la clave si ya hay un intento en curso; si no, genera una nueva.
    if (!idempotenciaKey.current) {
      idempotenciaKey.current = crypto.randomUUID()
    }

    setCargando(true)
    try {
      const data = await registrarAporte({
        afiliadoId: form.afiliadoId.trim(),
        monto,
        canal: form.canal,
        fecha: form.fecha || undefined,
        idempotenciaKey: idempotenciaKey.current,
      })
      setResultado(data)
      idempotenciaKey.current = null // éxito: el próximo aporte usa una clave nueva
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
          Fecha (opcional, por defecto hoy)
          <input
            type="date"
            value={form.fecha}
            onChange={e => actualizar('fecha', e.target.value)}
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
            {CANALES.map(c => <option key={c} value={c}>{c}</option>)}
          </select>
        </label>

        <button type="submit" disabled={cargando}>
          {cargando ? 'Registrando...' : 'Registrar'}
        </button>
      </form>

      {error && (
        <p style={{ color: 'crimson', marginTop: 16 }}>Error: {error}</p>
      )}

      {resultado && (
        <div style={{ marginTop: 16, padding: 12, background: '#f0f0f0' }}>
          <p>Aporte registrado. ID: <strong>{resultado.id}</strong> — Estado: <strong>{resultado.estado}</strong></p>
          {resultado.marcadaRevision && (
            <p style={{ color: '#b8860b' }}>
              Superó el umbral: quedó <strong>marcado para revisión</strong>. Ya reserva cupo del tope mensual y queda pendiente de aprobación.
            </p>
          )}
        </div>
      )}
    </div>
  )
}
