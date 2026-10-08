import { useLayoutEffect, useRef, useState } from 'react'
import { registrarAporte } from '../api/aportesApi'
import { CANALES, etiquetaCanal, formatearCOP, mostrarMonto, normalizarMonto } from '../formato'

const FORM_INICIAL = { afiliadoId: '', monto: '', canal: 'APP_MOVIL' }
const MONTO_VALIDO = /^\d{1,13}(\.\d{1,2})?$/

/**
 * Formulario de registro de aporte.
 *
 * Idempotencia: la clave se genera una vez por "intento de aporte" y se conserva
 * mientras los datos no cambien. Si la respuesta se pierde (timeout, red) y el usuario
 * vuelve a enviar, el servidor reconoce la clave y no duplica el aporte.
 * Se regenera solo cuando el usuario modifica el formulario o tras un registro exitoso.
 */
export default function RegistrarAporte() {
  const [form, setForm] = useState(FORM_INICIAL)
  const [idempotenciaKey, setIdempotenciaKey] = useState(() => crypto.randomUUID())
  const [resultado, setResultado] = useState(null)
  const [error, setError] = useState(null)
  const [cargando, setCargando] = useState(false)

  // Al insertar separadores de miles el texto cambia de largo y el cursor saltaría al final;
  // se recuerda cuántos dígitos/comas había antes del cursor y se restaura tras el render.
  const montoRef = useRef(null)
  const caretMonto = useRef(null)

  useLayoutEffect(() => {
    const input = montoRef.current
    if (caretMonto.current === null || !input) return
    let significativos = 0
    let posicion = 0
    while (posicion < input.value.length && significativos < caretMonto.current) {
      if (/[\d,]/.test(input.value[posicion])) significativos++
      posicion++
    }
    input.setSelectionRange(posicion, posicion)
    caretMonto.current = null
  }, [form.monto])

  function cambiarMonto(e) {
    const { value, selectionStart } = e.target
    caretMonto.current = value.slice(0, selectionStart).replace(/[^\d,]/g, '').length
    actualizar('monto', normalizarMonto(value))
  }

  function actualizar(campo, valor) {
    setForm(f => ({ ...f, [campo]: valor }))
    setIdempotenciaKey(crypto.randomUUID())
  }

  function validar() {
    const afiliadoId = form.afiliadoId.trim()
    if (!/^[A-Za-z0-9-]{1,50}$/.test(afiliadoId)) {
      return 'El ID de afiliado solo admite letras, números y guiones'
    }
    const monto = form.monto.trim()
    if (!MONTO_VALIDO.test(monto)) {
      return 'El monto debe ser un número con máximo 2 decimales (usa la coma para los decimales)'
    }
    if (!/[1-9]/.test(monto)) {
      return 'El monto debe ser mayor a cero'
    }
    return null
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    setResultado(null)

    const errorValidacion = validar()
    if (errorValidacion) {
      setError(errorValidacion)
      return
    }

    setCargando(true)
    try {
      const data = await registrarAporte({
        afiliadoId: form.afiliadoId.trim(),
        monto: form.monto.trim(), // string: BigDecimal en el backend, sin errores de punto flotante
        canal: form.canal,
        idempotenciaKey,
      })
      setResultado(data)
      setForm(FORM_INICIAL)
      setIdempotenciaKey(crypto.randomUUID())
    } catch (err) {
      setError(err.message)
    } finally {
      setCargando(false)
    }
  }

  return (
    <section className="card">
      <div className="card__header">
        <h2 className="card__title">Registrar aporte</h2>
        <p className="card__subtitle">Registra un aporte voluntario. La fecha la asigna el sistema al momento del registro.</p>
      </div>

      <form onSubmit={handleSubmit} noValidate className="form-grid">
        <label className="field">
          <span className="field__label">ID del afiliado</span>
          <input
            className="input"
            value={form.afiliadoId}
            onChange={e => actualizar('afiliadoId', e.target.value)}
            placeholder="AF-001"
            maxLength={50}
            autoComplete="off"
            required
          />
        </label>

        <label className="field">
          <span className="field__label">Monto</span>
          <div className="input-money">
            <span className="input-money__prefix">$</span>
            <input
              className="input"
              inputMode="decimal"
              ref={montoRef}
              value={mostrarMonto(form.monto)}
              onChange={cambiarMonto}
              placeholder="500.000,00"
              autoComplete="off"
              required
            />
            <span className="input-money__suffix">COP</span>
          </div>
        </label>

        <fieldset className="field field--full" style={{ border: 0, padding: 0, margin: 0 }}>
          <legend className="field__label" style={{ marginBottom: 6 }}>Canal de origen</legend>
          <div className="choice-group">
            {CANALES.map(c => (
              <label key={c.valor} className="choice">
                <input
                  type="radio"
                  name="canal"
                  value={c.valor}
                  checked={form.canal === c.valor}
                  onChange={e => actualizar('canal', e.target.value)}
                />
                {c.etiqueta}
              </label>
            ))}
          </div>
        </fieldset>

        <div className="form-actions">
          <p className="form-note">Los aportes que superen el umbral de revisión quedan marcados para revisión posterior.</p>
          <button type="submit" className="btn btn--primary" disabled={cargando}>
            {cargando && <span className="spinner" aria-hidden="true" />}
            {cargando ? 'Registrando…' : 'Registrar aporte'}
          </button>
        </div>
      </form>

      {error && (
        <div role="alert" className="alert alert--error">
          <span className="alert__icon" aria-hidden="true">!</span>
          <div>
            <p className="alert__title">No se pudo registrar el aporte</p>
            <p className="alert__body">{error}</p>
          </div>
        </div>
      )}

      {resultado && (
        <>
          <div role="status" className="alert alert--success">
            <span className="alert__icon" aria-hidden="true">✓</span>
            <div style={{ flex: 1 }}>
              <p className="alert__title">
                {resultado.reintento ? 'Este aporte ya estaba registrado' : 'Aporte registrado correctamente'}
              </p>
              <p className="alert__body">
                {resultado.reintento
                  ? 'Se reconoció un reenvío del mismo aporte; no se duplicó.'
                  : `Aporte #${resultado.aporte.id} para el afiliado ${resultado.aporte.afiliadoId}.`}
              </p>
              <dl className="detail-list">
                <div><dt>Monto</dt><dd>{formatearCOP(resultado.aporte.monto)}</dd></div>
                <div><dt>Fecha</dt><dd>{resultado.aporte.fecha}</dd></div>
                <div><dt>Periodo</dt><dd>{resultado.aporte.periodo}</dd></div>
                <div><dt>Canal</dt><dd>{etiquetaCanal(resultado.aporte.canal)}</dd></div>
              </dl>
            </div>
          </div>

          {resultado.aporte.marcadaRevision && (
            <div className="alert alert--warning">
              <span className="alert__icon" aria-hidden="true">!</span>
              <div>
                <p className="alert__title">Marcado para revisión posterior</p>
                <p className="alert__body">El monto supera el umbral de revisión aplicable al afiliado y al canal.</p>
              </div>
            </div>
          )}
        </>
      )}
    </section>
  )
}
