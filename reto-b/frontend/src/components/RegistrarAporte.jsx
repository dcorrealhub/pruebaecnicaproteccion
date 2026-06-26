import { useState, useCallback } from 'react'
import { registrarAporte } from '../api/aportesApi'
import styles from '../styles/RegistrarAporte.module.css'

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
      <h2 className={styles.title}>Registrar aporte</h2>

      <form onSubmit={handleSubmit} className={styles.form}>
        <label className={styles.label}>
          ID Afiliado
          <input
            value={form.afiliadoId}
            onChange={e => actualizarCampo('afiliadoId', e.target.value)}
            placeholder="AF-001"
            required
            className={styles.input}
          />
          {errores.afiliadoId && (
            <span role="alert" className={styles.fieldError}>{errores.afiliadoId}</span>
          )}
        </label>

        <label className={styles.label}>
          Monto (COP)
          <input
            type="number"
            min="0.01"
            step="0.01"
            value={form.monto}
            onChange={e => actualizarCampo('monto', e.target.value)}
            placeholder="0.00"
            required
            className={styles.input}
          />
          {errores.monto && (
            <span role="alert" className={styles.fieldError}>{errores.monto}</span>
          )}
        </label>

        <label className={styles.label}>
          Canal
          <select
            value={form.canal}
            onChange={e => setForm(f => ({ ...f, canal: e.target.value }))}
            className={styles.select}
          >
            <option value="APP_MOVIL">App móvil</option>
            <option value="WEB">Web</option>
            <option value="SUCURSAL">Sucursal</option>
          </select>
        </label>

        <button type="submit" disabled={cargando} className={styles.submitButton}>
          {cargando ? 'Registrando...' : 'Registrar'}
        </button>
      </form>

      {error && (
        <p role="alert" className={styles.serverError}>{error}</p>
      )}

      {resultado && (
        <div role="status" className={styles.successCard}>
          <p className={styles.successText}>Aporte registrado correctamente.</p>
          <p className={styles.successId}>ID: {resultado.id}</p>
          {resultado.marcadaRevision && (
            <p className={styles.revisionWarning}>
              Este aporte superó el umbral definido y quedó marcado para revisión.
            </p>
          )}
        </div>
      )}
    </div>
  )
}
