import { useState } from 'react'
import { registrarAporte } from '../api/aportesApi'

const MONTO_MIN = 0.01
const MONTO_MAX = 10_000_000

function formatCOP(value) {
  return Number(value).toLocaleString('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })
}

export default function RegistrarAporte() {
  const [form, setForm] = useState({ afiliadoId: '', monto: '', canal: 'APP_MOVIL' })
  const [erroresCampo, setErroresCampo] = useState({})
  const [resultado, setResultado] = useState(null)
  const [alerta, setAlerta] = useState(null) // { tipo: 'success'|'error'|'warning'|'info', titulo, mensaje }
  const [cargando, setCargando] = useState(false)

  function validar() {
    const errs = {}
    if (!form.afiliadoId.trim()) errs.afiliadoId = 'El ID del afiliado es obligatorio'
    const monto = parseFloat(form.monto)
    if (!form.monto || isNaN(monto)) errs.monto = 'Ingresa un monto valido'
    else if (monto < MONTO_MIN) errs.monto = 'El monto debe ser mayor a $0'
    if (!form.canal) errs.canal = 'Selecciona un canal'
    return errs
  }

  function handleChange(campo, valor) {
    setForm(f => ({ ...f, [campo]: valor }))
    if (erroresCampo[campo]) setErroresCampo(e => ({ ...e, [campo]: undefined }))
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setAlerta(null)
    setResultado(null)

    const errs = validar()
    if (Object.keys(errs).length > 0) {
      setErroresCampo(errs)
      return
    }

    setCargando(true)
    try {
      const data = await registrarAporte({
        afiliadoId: form.afiliadoId.trim(),
        monto: parseFloat(form.monto),
        canal: form.canal,
        idempotenciaKey: crypto.randomUUID(),
      })
      setResultado(data)

      if (data.marcadaRevision) {
        setAlerta({
          tipo: 'warning',
          titulo: 'Aporte registrado — Requiere Revision',
          mensaje: `El aporte de ${formatCOP(data.monto)} supera el umbral de revision. Ha sido marcado para revision por el equipo de cumplimiento.`,
        })
      } else {
        setAlerta({
          tipo: 'success',
          titulo: 'Aporte registrado exitosamente',
          mensaje: `El aporte de ${formatCOP(data.monto)} fue registrado con ID #${data.id} para el periodo ${data.periodo}.`,
        })
      }

      setForm({ afiliadoId: '', monto: '', canal: 'APP_MOVIL' })
    } catch (err) {
      const msg = err.message || 'Error inesperado'
      if (msg.includes('tope mensual')) {
        setAlerta({ tipo: 'error', titulo: 'Tope mensual superado', mensaje: msg })
      } else if (msg.includes('concurrencia')) {
        setAlerta({ tipo: 'info', titulo: 'Conflicto de concurrencia', mensaje: 'El registro no pudo completarse por concurrencia simultanea. Reintenta en unos segundos.' })
      } else {
        setAlerta({ tipo: 'error', titulo: 'Error al registrar', mensaje: msg })
      }
    } finally {
      setCargando(false)
    }
  }

  const iconos = { success: '✓', warning: '⚠', error: '✕', info: 'ℹ' }

  return (
    <div className="card">
      <h2 className="card__title">Registrar aporte voluntario</h2>

      <form onSubmit={handleSubmit} noValidate>
        <div className="form-grid form-grid--single">

          <div className="form-field">
            <label className="form-label" htmlFor="afiliadoId">ID Afiliado</label>
            <input
              id="afiliadoId"
              className={`form-input ${erroresCampo.afiliadoId ? 'form-input--error' : ''}`}
              value={form.afiliadoId}
              onChange={e => handleChange('afiliadoId', e.target.value)}
              placeholder="Ej: AF-001"
              autoComplete="off"
            />
            {erroresCampo.afiliadoId && <span className="field-error">{erroresCampo.afiliadoId}</span>}
          </div>

          <div className="form-field">
            <label className="form-label" htmlFor="monto">Monto (COP)</label>
            <input
              id="monto"
              type="number"
              min={MONTO_MIN}
              max={MONTO_MAX}
              step="0.01"
              className={`form-input ${erroresCampo.monto ? 'form-input--error' : ''}`}
              value={form.monto}
              onChange={e => handleChange('monto', e.target.value)}
              placeholder="Ej: 3000000"
            />
            {erroresCampo.monto && <span className="field-error">{erroresCampo.monto}</span>}
            {form.monto && !isNaN(parseFloat(form.monto)) && !erroresCampo.monto && (
              <span style={{ fontSize: 12, color: '#64748b', marginTop: 2 }}>
                {formatCOP(form.monto)}
              </span>
            )}
          </div>

          <div className="form-field">
            <label className="form-label" htmlFor="canal">Canal de origen</label>
            <select
              id="canal"
              className={`form-select ${erroresCampo.canal ? 'form-input--error' : ''}`}
              value={form.canal}
              onChange={e => handleChange('canal', e.target.value)}
            >
              <option value="APP_MOVIL">App movil</option>
              <option value="WEB">Web</option>
              <option value="SUCURSAL">Sucursal</option>
            </select>
            {erroresCampo.canal && <span className="field-error">{erroresCampo.canal}</span>}
          </div>

        </div>

        <button type="submit" className="btn btn--primary" disabled={cargando}>
          {cargando ? 'Registrando...' : 'Registrar aporte'}
        </button>
      </form>

      {alerta && (
        <div className={`alert alert--${alerta.tipo}`}>
          <span className="alert__icon">{iconos[alerta.tipo]}</span>
          <div>
            <span className="alert__title">{alerta.titulo}</span>
            {alerta.mensaje}
          </div>
        </div>
      )}

      {resultado && (
        <div className="result-card">
          <div className="result-card__row">
            <span className="result-card__label">ID del aporte</span>
            <span className="result-card__value">#{resultado.id}</span>
          </div>
          <div className="result-card__row">
            <span className="result-card__label">Afiliado</span>
            <span className="result-card__value">{resultado.afiliadoId}</span>
          </div>
          <div className="result-card__row">
            <span className="result-card__label">Monto registrado</span>
            <span className="result-card__value">{formatCOP(resultado.monto)}</span>
          </div>
          <div className="result-card__row">
            <span className="result-card__label">Fecha</span>
            <span className="result-card__value">{resultado.fecha}</span>
          </div>
          <div className="result-card__row">
            <span className="result-card__label">Canal</span>
            <span className="result-card__value">{resultado.canal}</span>
          </div>
          <div className="result-card__row">
            <span className="result-card__label">Periodo</span>
            <span className="result-card__value">{resultado.periodo}</span>
          </div>
          <div className="result-card__row">
            <span className="result-card__label">Estado</span>
            <span className="result-card__value">
              <span className={`badge ${resultado.marcadaRevision ? 'badge--revision' : 'badge--ok'}`}>
                {resultado.marcadaRevision ? 'Requiere revision' : 'Aprobado'}
              </span>
            </span>
          </div>
        </div>
      )}
    </div>
  )
}
