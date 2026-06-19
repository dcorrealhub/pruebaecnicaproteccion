import { useRef, useState } from 'react'
import { registrarAporte } from '../api/aportesApi'

const HOY = new Date().toISOString().split('T')[0]

export default function RegistrarAporte() {
  const [form, setForm] = useState({ afiliadoId: '', monto: '', fecha: HOY, canal: 'APP_MOVIL' })
  const [errores, setErrores] = useState({})
  const [resultado, setResultado] = useState(null)
  const [errorServidor, setErrorServidor] = useState(null)
  const [cargando, setCargando] = useState(false)
  const idempotenciaKey = useRef(crypto.randomUUID())

  function campo(nombre) {
    return {
      value: form[nombre],
      onChange: e => {
        setForm(f => ({ ...f, [nombre]: e.target.value }))
        setErrores(prev => ({ ...prev, [nombre]: null }))
      },
    }
  }

  function validar() {
    const errs = {}
    if (!form.afiliadoId.trim()) errs.afiliadoId = 'El ID de afiliado es obligatorio'
    const monto = parseFloat(form.monto)
    if (!form.monto || isNaN(monto) || monto <= 0) errs.monto = 'Ingresá un monto mayor a cero'
    if (!form.fecha) errs.fecha = 'La fecha es obligatoria'
    return errs
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setErrorServidor(null)
    setResultado(null)

    const errs = validar()
    if (Object.keys(errs).length > 0) { setErrores(errs); return }

    setCargando(true)
    try {
      const data = await registrarAporte(
        { ...form, monto: parseFloat(form.monto) },
        idempotenciaKey.current
      )
      setResultado(data)
      idempotenciaKey.current = crypto.randomUUID()
    } catch (err) {
      setErrorServidor(err.message)
    } finally {
      setCargando(false)
    }
  }

  return (
    <div className="card">
      <h2 className="card-title">Registrar aporte</h2>

      <form onSubmit={handleSubmit} noValidate>
        <div className="form-grid">
          <div className="form-field">
            <label className="form-label" htmlFor="afiliadoId">ID Afiliado (sintético)</label>
            <input
              id="afiliadoId"
              className={`form-input${errores.afiliadoId ? ' error' : ''}`}
              placeholder="AF-001"
              data-testid="input-afiliado"
              {...campo('afiliadoId')}
            />
            {errores.afiliadoId && <span className="field-error">{errores.afiliadoId}</span>}
          </div>

          <div className="form-field">
            <label className="form-label" htmlFor="monto">Monto (COP)</label>
            <input
              id="monto"
              type="number"
              min="0.01"
              step="0.01"
              className={`form-input${errores.monto ? ' error' : ''}`}
              placeholder="100000"
              data-testid="input-monto"
              {...campo('monto')}
            />
            {errores.monto && <span className="field-error">{errores.monto}</span>}
          </div>

          <div className="form-field">
            <label className="form-label" htmlFor="fecha">Fecha del aporte</label>
            <input
              id="fecha"
              type="date"
              max={HOY}
              className={`form-input${errores.fecha ? ' error' : ''}`}
              data-testid="input-fecha"
              {...campo('fecha')}
            />
            {errores.fecha && <span className="field-error">{errores.fecha}</span>}
          </div>

          <div className="form-field">
            <label className="form-label" htmlFor="canal">Canal de origen</label>
            <select
              id="canal"
              className="form-select"
              data-testid="select-canal"
              {...campo('canal')}
            >
              <option value="APP_MOVIL">App móvil</option>
              <option value="WEB">Web</option>
              <option value="SUCURSAL">Sucursal</option>
            </select>
          </div>

          <div className="form-field full-width" style={{ marginTop: 4 }}>
            <button type="submit" className="btn btn-primary" disabled={cargando} data-testid="btn-registrar">
              {cargando ? 'Registrando…' : 'Registrar aporte'}
            </button>
          </div>
        </div>
      </form>

      {errorServidor && (
        <div className="alert alert-error" role="alert">{errorServidor}</div>
      )}

      {resultado && (
        <div className="alert alert-success" role="status">
          Aporte registrado correctamente — Periodo: <strong>{resultado.periodo}</strong>
          {resultado.marcadaRevision && (
            <div className="alert alert-warning">
              <span className="badge-revision">⚠ Requiere revisión</span>
              {' '}Este aporte superó el umbral y quedó marcado para revisión por el área de cumplimiento.
            </div>
          )}
        </div>
      )}
    </div>
  )
}
