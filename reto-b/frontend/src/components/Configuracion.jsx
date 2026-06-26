import { useEffect, useState } from 'react'
import { obtenerParametros, actualizarParametros } from '../api/aportesApi'

const COP = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })

/**
 * Pantalla de configuración de los parámetros globales (tope mensual y umbral de
 * revisión). Carga los valores actuales al montar y permite guardarlos. Los cambios
 * aplican en runtime al registro de aportes.
 */
export default function Configuracion() {
  const [form, setForm] = useState({ topeMensual: '', umbralRevision: '' })
  const [cargando, setCargando] = useState(true)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState(null)
  const [ok, setOk] = useState(false)

  useEffect(() => {
    obtenerParametros()
      .then(p => setForm({ topeMensual: String(p.topeMensual), umbralRevision: String(p.umbralRevision) }))
      .catch(err => setError(err.message))
      .finally(() => setCargando(false))
  }, [])

  function actualizar(campo, valor) {
    setForm(f => ({ ...f, [campo]: valor }))
    setOk(false)
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    setOk(false)

    const tope = parseFloat(form.topeMensual)
    const umbral = parseFloat(form.umbralRevision)
    if (!(tope > 0) || !(umbral > 0)) {
      setError('El tope y el umbral deben ser mayores a cero')
      return
    }
    if (umbral > tope) {
      setError('El umbral de revisión no puede ser mayor que el tope mensual')
      return
    }

    setGuardando(true)
    try {
      const p = await actualizarParametros({ topeMensual: tope, umbralRevision: umbral })
      setForm({ topeMensual: String(p.topeMensual), umbralRevision: String(p.umbralRevision) })
      setOk(true)
    } catch (err) {
      setError(err.message)
    } finally {
      setGuardando(false)
    }
  }

  if (cargando) return <p>Cargando configuración...</p>

  const topeNum = parseFloat(form.topeMensual)
  const umbralNum = parseFloat(form.umbralRevision)

  return (
    <div>
      <h2 style={{ fontSize: 18, marginBottom: 8 }}>Configuración de parámetros</h2>
      <p style={{ color: '#555', marginBottom: 16, maxWidth: 520 }}>
        Estos valores aplican a todos los afiliados. Un aporte que supere el <strong>umbral</strong>
        {' '}queda marcado para revisión; el <strong>tope mensual</strong> limita el acumulado por afiliado.
      </p>

      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 12, maxWidth: 400 }}>
        <label>
          Tope mensual (COP)
          <input
            type="number" min="0.01" step="0.01"
            value={form.topeMensual}
            onChange={e => actualizar('topeMensual', e.target.value)}
            required
            style={{ display: 'block', width: '100%', marginTop: 4 }}
          />
          {topeNum > 0 && <small style={{ color: '#888' }}>{COP.format(topeNum)}</small>}
        </label>

        <label>
          Umbral de revisión (COP)
          <input
            type="number" min="0.01" step="0.01"
            value={form.umbralRevision}
            onChange={e => actualizar('umbralRevision', e.target.value)}
            required
            style={{ display: 'block', width: '100%', marginTop: 4 }}
          />
          {umbralNum > 0 && <small style={{ color: '#888' }}>{COP.format(umbralNum)}</small>}
        </label>

        <button type="submit" disabled={guardando}>
          {guardando ? 'Guardando...' : 'Guardar'}
        </button>
      </form>

      {error && <p style={{ color: 'crimson', marginTop: 16 }}>Error: {error}</p>}
      {ok && <p style={{ color: 'green', marginTop: 16 }}>Parámetros actualizados correctamente.</p>}
    </div>
  )
}
