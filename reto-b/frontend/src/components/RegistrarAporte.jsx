import { useRef, useState } from 'react'
import { registrarAporte } from '../api/aportesApi'
import MensajeError from './MensajeError'
import { CANALES, formatoCOP, formatoFecha } from '../formato'

/**
 * Formulario de registro de aporte.
 *
 * La idempotenciaKey se genera en el cliente y se conserva mientras el usuario reintenta
 * el MISMO envío (p. ej. tras un error de red), para que el reintento no duplique el aporte.
 * Se renueva al registrar con éxito o al cambiar cualquier campo (es otra operación).
 *
 * @param {{ onRegistrado?: (aporte: object) => void }} props
 */
export default function RegistrarAporte({ onRegistrado }) {
  const [form, setForm] = useState({ afiliadoId: '', monto: '', canal: 'APP_MOVIL' })
  const [resultado, setResultado] = useState(null)
  const [error, setError] = useState(null)
  const [cargando, setCargando] = useState(false)
  const idempotenciaKey = useRef(crypto.randomUUID())

  function actualizar(campo, valor) {
    setForm(f => ({ ...f, [campo]: valor }))
    idempotenciaKey.current = crypto.randomUUID()
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    setResultado(null)

    const monto = Number(form.monto)
    if (!(monto > 0)) {
      setError({ message: 'El monto debe ser mayor a cero' })
      return
    }

    setCargando(true)
    try {
      const data = await registrarAporte({
        afiliadoId: form.afiliadoId.trim(),
        monto,
        canal: form.canal,
        idempotenciaKey: idempotenciaKey.current,
      })
      setResultado(data)
      idempotenciaKey.current = crypto.randomUUID()
      onRegistrado?.(data.aporte)
    } catch (err) {
      setError(err)
    } finally {
      setCargando(false)
    }
  }

  return (
    <section className="card">
      <h2 className="card__title">Registrar aporte</h2>
      <p className="card__subtitle">La fecha y el periodo los asigna el sistema al momento del registro.</p>

      <form onSubmit={handleSubmit} className="form-grid">
        <label className="field">
          <span className="field__label">ID Afiliado (sintético)</span>
          <input
            className="input"
            value={form.afiliadoId}
            onChange={e => actualizar('afiliadoId', e.target.value)}
            placeholder="AF-001"
            required
          />
        </label>

        <label className="field">
          <span className="field__label">Monto (COP)</span>
          <input
            className="input"
            type="number"
            min="0.01"
            step="0.01"
            value={form.monto}
            onChange={e => actualizar('monto', e.target.value)}
            placeholder="1500000"
            required
          />
        </label>

        <label className="field">
          <span className="field__label">Canal</span>
          <select
            className="input"
            value={form.canal}
            onChange={e => actualizar('canal', e.target.value)}
          >
            {Object.entries(CANALES).map(([valor, etiqueta]) => (
              <option key={valor} value={valor}>{etiqueta}</option>
            ))}
          </select>
        </label>

        <button type="submit" className="btn" disabled={cargando}>
          {cargando ? 'Registrando…' : 'Registrar aporte'}
        </button>
      </form>

      <MensajeError error={error} />

      {resultado && (
        <div role="status" className={`alert ${resultado.duplicado ? 'alert--info' : 'alert--success'}`}>
          <p>
            {resultado.duplicado
              ? <>Este aporte ya estaba registrado (ID {resultado.aporte.id}); no se duplicó.</>
              : <>
                  <strong>Aporte registrado</strong> · ID {resultado.aporte.id} ·{' '}
                  {formatoCOP(resultado.aporte.monto)} · {formatoFecha(resultado.aporte.fecha)} · periodo {resultado.aporte.periodo}
                </>}
          </p>
          {resultado.aporte.marcadaRevision && (
            <p className="alert__warning">⚠ Este aporte quedó marcado para revisión.</p>
          )}
        </div>
      )}
    </section>
  )
}
