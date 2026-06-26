import { useState, useCallback } from 'react'
import { registrarAporte } from '../api/aportesApi'

const COLOR_ERROR = '#b91c1c'
const COLOR_WARNING = '#d97706'
const COLOR_MUTED = '#6b7280'

function validarCampos({ afiliadoId, monto }) {
  const errores = {}
  if (!afiliadoId.trim()) {
    errores.afiliadoId = 'El ID del afiliado es obligatorio.'
  }
  const montoNum = parseFloat(monto)
  if (!monto || isNaN(montoNum) || montoNum <= 0) {
    errores.monto = 'El monto debe ser un número positivo mayor a cero.'
  }
  return errores
}

export default function RegistrarAporte() {
  const [form, setForm] = useState({ afiliadoId: '', monto: '', canal: 'APP_MOVIL' })
  const [resultado, setResultado] = useState(null)
  const [error, setError] = useState(null)
  const [cargando, setCargando] = useState(false)
  const [errores, setErrores] = useState({})

  const actualizarCampo = useCallback((campo, valor) => {
    setForm(f => ({ ...f, [campo]: valor }))
    setErrores(e => {
      if (!e[campo]) return e
      const next = { ...e }
      delete next[campo]
      return next
    })
  }, [])

  function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    setResultado(null)

    const v = validarCampos(form)
    setErrores(v)
    if (Object.keys(v).length > 0) return

    setCargando(true)

    registrarAporte({
      ...form,
      monto: parseFloat(form.monto),
      idempotenciaKey: crypto.randomUUID(),
    })
      .then(data => {
        setResultado(data)
        setForm(f => ({ ...f, monto: '' }))
      })
      .catch(err => setError(err.message))
      .finally(() => setCargando(false))
  }

  return (
    <div>
      <h2 style={{ fontSize: 18, marginBottom: 16 }}>Registrar aporte</h2>

      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 12, maxWidth: 400 }}>
        <label>
          ID Afiliado
          <input
            value={form.afiliadoId}
            onChange={e => actualizarCampo('afiliadoId', e.target.value)}
            placeholder="AF-001"
            required
            style={{ display: 'block', width: '100%', marginTop: 4 }}
          />
          {errores.afiliadoId && (
            <span role="alert" style={{ color: COLOR_ERROR, fontSize: 13 }}>{errores.afiliadoId}</span>
          )}
        </label>

        <label>
          Monto (COP)
          <input
            type="number"
            min="0.01"
            step="0.01"
            value={form.monto}
            onChange={e => actualizarCampo('monto', e.target.value)}
            placeholder="0.00"
            required
            style={{ display: 'block', width: '100%', marginTop: 4 }}
          />
          {errores.monto && (
            <span role="alert" style={{ color: COLOR_ERROR, fontSize: 13 }}>{errores.monto}</span>
          )}
        </label>

        <label>
          Canal
          <select
            value={form.canal}
            onChange={e => setForm(f => ({ ...f, canal: e.target.value }))}
            style={{ display: 'block', width: '100%', marginTop: 4 }}
          >
            <option value="APP_MOVIL">App móvil</option>
            <option value="WEB">Web</option>
            <option value="SUCURSAL">Sucursal</option>
          </select>
        </label>

        <button type="submit" disabled={cargando} style={{ alignSelf: 'flex-start' }}>
          {cargando ? 'Registrando...' : 'Registrar'}
        </button>
      </form>

      {error && (
        <p role="alert" style={{ color: COLOR_ERROR, marginTop: 16 }}>{error}</p>
      )}

      {resultado && (
        <div role="status" style={{ marginTop: 16, padding: 16, border: '1px solid #d1d5db', borderRadius: 4 }}>
          <p style={{ margin: 0 }}>Aporte registrado correctamente.</p>
          <p style={{ margin: '4px 0 0', fontSize: 13, color: COLOR_MUTED }}>ID: {resultado.id}</p>
          {resultado.marcadaRevision && (
            <p style={{ color: COLOR_WARNING, margin: '8px 0 0', fontWeight: 500 }}>
              Este aporte superó el umbral definido y quedó marcado para revisión.
            </p>
          )}
        </div>
      )}
    </div>
  )
}
