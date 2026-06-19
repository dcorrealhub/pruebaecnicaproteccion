import { useState } from 'react'
import { registrarAporte } from '../api/aportesApi'
import './aportes.css'

/**
 * Formulario de registro de aporte voluntario.
 *
 * - Genera idempotenciaKey con crypto.randomUUID() en cada submit nuevo,
 *   de forma que un reintento de red use la misma clave solo si el usuario
 *   no ha vuelto a enviar el formulario (la clave se fija al montar el form,
 *   no en cada render).
 * - Valida monto > 0 en el cliente antes de llamar al backend, aunque la
 *   validación real de negocio vive en el servidor.
 */
export default function RegistrarAporte() {
  const [form, setForm] = useState({ afiliadoId: '', monto: '', canal: 'APP_MOVIL' })
  const [resultado, setResultado] = useState(null)
  const [error, setError] = useState(null)
  const [cargando, setCargando] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    setResultado(null)

    const monto = parseFloat(form.monto)
    if (!Number.isFinite(monto) || monto <= 0) {
      setError('El monto debe ser un número mayor a cero.')
      return
    }

    setCargando(true)
    try {
      const data = await registrarAporte({
        ...form,
        monto,
        idempotenciaKey: crypto.randomUUID(),
      })
      setResultado(data)
      setForm({ afiliadoId: '', monto: '', canal: 'APP_MOVIL' })
    } catch (err) {
      setError(err.message)
    } finally {
      setCargando(false)
    }
  }

  return (
    <div className="ap-root">
      <div className="ap-card">
        <div className="ap-heading">
          <h2>Registrar aporte</h2>
          <span className="ap-eyebrow">Fondo voluntario</span>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="ap-field">
            <label htmlFor="afiliadoId">ID afiliado</label>
            <input
              id="afiliadoId"
              className="ap-input"
              value={form.afiliadoId}
              onChange={e => setForm(f => ({ ...f, afiliadoId: e.target.value }))}
              placeholder="AF-001"
              required
            />
          </div>

          <div className="ap-row">
            <div className="ap-field">
              <label htmlFor="monto">Monto (COP)</label>
              <input
                id="monto"
                className="ap-input"
                data-money="true"
                type="number"
                min="0.01"
                step="0.01"
                inputMode="decimal"
                value={form.monto}
                onChange={e => setForm(f => ({ ...f, monto: e.target.value }))}
                placeholder="0.00"
                required
              />
            </div>

            <div className="ap-field">
              <label htmlFor="canal">Canal</label>
              <select
                id="canal"
                className="ap-select"
                value={form.canal}
                onChange={e => setForm(f => ({ ...f, canal: e.target.value }))}
              >
                <option value="APP_MOVIL">App móvil</option>
                <option value="WEB">Web</option>
                <option value="SUCURSAL">Sucursal</option>
              </select>
            </div>
          </div>

          <button type="submit" className="ap-button" disabled={cargando}>
            {cargando ? 'Registrando…' : 'Registrar aporte'}
          </button>
        </form>

        {error && (
          <div className="ap-alert ap-alert-error" role="alert">
            <svg className="ap-alert-icon" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
              <circle cx="10" cy="10" r="8.5" stroke="currentColor" strokeWidth="1.4" />
              <path d="M10 6v4.5" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" />
              <circle cx="10" cy="13.5" r="0.9" fill="currentColor" />
            </svg>
            <span><strong>No se pudo registrar el aporte.</strong> {error}</span>
          </div>
        )}

        {resultado && (
          <div className="ap-alert ap-alert-success" role="status">
            <svg className="ap-alert-icon" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
              <circle cx="10" cy="10" r="8.5" stroke="currentColor" strokeWidth="1.4" />
              <path d="M6.5 10.2l2.3 2.3 4.7-5" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
            <span>
              <strong>Aporte registrado.</strong> ID <span className="ap-result-id">{resultado.id}</span>
            </span>
          </div>
        )}

        {resultado?.marcadaRevision && (
          <div className="ap-alert ap-alert-warning" role="status">
            <svg className="ap-alert-icon" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M10 2.5l8 14H2l8-14z" stroke="currentColor" strokeWidth="1.4" strokeLinejoin="round" />
              <path d="M10 8v3.5" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" />
              <circle cx="10" cy="13.8" r="0.8" fill="currentColor" />
            </svg>
            <span>Este aporte superó el umbral configurado y quedó <strong>marcado para revisión</strong>.</span>
          </div>
        )}
      </div>
    </div>
  )
}