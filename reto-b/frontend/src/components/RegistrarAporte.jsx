import { useState } from 'react'
import { registrarAporte } from '../api/aportesApi'

export default function RegistrarAporte() {
  const [form, setForm] = useState({ afiliadoId: '', monto: '', canal: 'APP_MOVIL' })
  const [resultado, setResultado] = useState(null)
  const [error, setError] = useState(null)
  const [cargando, setCargando] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null); setResultado(null); setCargando(true)
    try {
      const data = await registrarAporte({
        ...form,
        monto: parseFloat(form.monto),
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
    <div style={s.card}>
      <form onSubmit={handleSubmit}>
        <div style={s.grid}>
          <Field label="ID Afiliado" hint="Ej: AF-001">
            <input style={s.input}
              value={form.afiliadoId}
              onChange={e => setForm(f => ({ ...f, afiliadoId: e.target.value }))}
              placeholder="AF-001" required />
          </Field>

          <Field label="Monto (COP)" hint="Debe ser mayor a $0">
            <input style={s.input} type="number" min="0.01" step="0.01"
              value={form.monto}
              onChange={e => setForm(f => ({ ...f, monto: e.target.value }))}
              placeholder="0.00" required />
          </Field>

          <Field label="Canal de origen">
            <select style={s.input} value={form.canal}
              onChange={e => setForm(f => ({ ...f, canal: e.target.value }))}>
              <option value="APP_MOVIL">App Móvil</option>
              <option value="WEB">Web</option>
              <option value="SUCURSAL">Sucursal</option>
            </select>
          </Field>
        </div>

        <div style={{ marginTop: 24 }}>
          <button type="submit" disabled={cargando} style={{
            ...s.btn,
            ...(cargando ? s.btnDisabled : {}),
          }}>
            {cargando ? 'Registrando...' : 'Registrar aporte'}
          </button>
        </div>
      </form>

      {error && (
        <div style={s.alertError}>
          <strong>Error:</strong> {error}
        </div>
      )}

      {resultado && (
        <div style={s.alertSuccess}>
          <div style={{ fontWeight: 600 }}>Aporte registrado — ID #{resultado.id}</div>
          <div style={{ fontSize: 13, marginTop: 4, color: '#374151' }}>
            {resultado.monto?.toLocaleString('es-CO', { style: 'currency', currency: 'COP' })}
            {' · '}{resultado.canal}{' · '}{resultado.fecha}
          </div>
          {resultado.marcadaRevision && (
            <div style={s.badge}>⚠ Marcado para revisión</div>
          )}
        </div>
      )}
    </div>
  )
}

function Field({ label, hint, children }) {
  return (
    <div>
      <label style={s.label}>{label}</label>
      {hint && <span style={s.hint}>{hint}</span>}
      {children}
    </div>
  )
}

const s = {
  card: {
    background: '#fff', borderRadius: 10,
    boxShadow: '0 1px 3px rgba(0,0,0,0.08)',
    padding: 28, maxWidth: 560,
  },
  grid: { display: 'grid', gap: 20 },
  label: { display: 'block', fontSize: 13, fontWeight: 600, color: '#374151', marginBottom: 2 },
  hint:  { display: 'block', fontSize: 12, color: '#9CA3AF', marginBottom: 6 },
  input: {
    width: '100%', padding: '9px 12px',
    border: '1.5px solid #D1D5DB', borderRadius: 6,
    fontSize: 14, color: '#111827',
    outline: 'none', boxSizing: 'border-box',
    transition: 'border-color 0.15s',
  },
  btn: {
    padding: '10px 24px', background: '#6CB531',
    color: '#fff', border: 'none', borderRadius: 6,
    fontSize: 14, fontWeight: 600, cursor: 'pointer',
  },
  btnDisabled: { background: '#B1B1B1', cursor: 'not-allowed' },
  alertError: {
    marginTop: 20, padding: '12px 16px',
    background: '#FEF2F2', border: '1px solid #FECACA',
    borderRadius: 6, color: '#B91C1C', fontSize: 14,
  },
  alertSuccess: {
    marginTop: 20, padding: '12px 16px',
    background: '#F0FDF4', border: '1px solid #BBF7D0',
    borderRadius: 6, fontSize: 14,
  },
  badge: {
    display: 'inline-block', marginTop: 8,
    padding: '3px 10px', background: '#FEF3C7',
    color: '#92400E', borderRadius: 20,
    fontSize: 12, fontWeight: 600,
  },
}