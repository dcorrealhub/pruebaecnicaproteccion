import { useState } from 'react'
import { registrarAporte } from '../api/aportesApi'
import Modal from './Modal'

export default function RegistrarAporte() {
  const [form, setForm] = useState({ afiliadoId: '', monto: '', canal: 'APP_MOVIL' })
  const [modal, setModal] = useState(null)
  const [cargando, setCargando] = useState(false)
  const [idempotenciaKey, setIdempotenciaKey] = useState(() => crypto.randomUUID())

  function cerrarModal() { setModal(null) }

  async function handleSubmit(e) {
    e.preventDefault()

    if (!form.afiliadoId.trim()) {
      setModal({ tipo: 'warning', titulo: 'Campo requerido', contenido: 'El ID del afiliado es obligatorio.' })
      return
    }
    const montoNum = parseFloat(form.monto.replace(/\./g, '').replace(',', '.'))
    if (!form.monto || isNaN(montoNum) || montoNum <= 0) {
      setModal({ tipo: 'warning', titulo: 'Monto inválido', contenido: 'El monto debe ser un número mayor a cero.' })
      return
    }

    setCargando(true)
    try {
      const data = await registrarAporte({
        afiliadoId: form.afiliadoId.trim().replace(/\s*-\s*/g, '-'),
        monto: montoNum,
        canal: form.canal,
        idempotenciaKey,
      })

      setModal({
        tipo: 'success',
        titulo: 'Aporte registrado',
        contenido: (
          <div>
            <p style={{ margin: '0 0 8px' }}><strong>ID:</strong> #{data.id}</p>
            <p style={{ margin: '0 0 8px' }}><strong>Afiliado:</strong> {data.afiliadoId}</p>
            <p style={{ margin: '0 0 8px' }}>
              <strong>Monto:</strong>{' '}
              {data.monto?.toLocaleString('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })}
            </p>
            <p style={{ margin: '0 0 8px' }}><strong>Canal:</strong> {data.canal}</p>
            <p style={{ margin: 0 }}><strong>Fecha:</strong> {data.fecha}</p>
            {data.marcadaRevision && (
              <div style={{
                marginTop: 14, padding: '8px 12px',
                background: '#FFFBEB', borderRadius: 6,
                border: '1px solid #FDE68A',
                color: '#92400E', fontSize: 13, fontWeight: 600,
              }}>
                ⚠ Este aporte quedó marcado para revisión posterior.
              </div>
            )}
          </div>
        ),
      })

      // Éxito — limpiar form y generar nueva key para el próximo aporte
      setForm({ afiliadoId: '', monto: '', canal: 'APP_MOVIL' })
      setIdempotenciaKey(crypto.randomUUID())

    } catch (err) {
      // Error — mantener la misma key para que el usuario pueda reintentar de forma segura
      setModal({
        tipo: 'error',
        titulo: 'Error al registrar',
        contenido: err.message || 'Ocurrió un error inesperado. Intentá de nuevo.',
      })
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
              placeholder="AF-001" />
          </Field>

          <Field label="Monto (COP)" hint="Ej: 3500000">
            <input style={s.input} type="text"
              value={form.monto}
              onChange={e => {
                const raw = e.target.value.replace(/[^\d]/g, '')
                setForm(f => ({ ...f, monto: raw }))
              }}
              placeholder="3500000" />
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
          <button type="submit" disabled={cargando}
            style={{ ...s.btn, ...(cargando ? s.btnDisabled : {}) }}>
            {cargando ? 'Registrando...' : 'Registrar aporte'}
          </button>
        </div>
      </form>

      {modal && (
        <Modal tipo={modal.tipo} titulo={modal.titulo} onClose={cerrarModal}>
          {typeof modal.contenido === 'string'
            ? <p style={{ margin: 0 }}>{modal.contenido}</p>
            : modal.contenido}
        </Modal>
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
    fontSize: 14, color: '#111827', outline: 'none',
    boxSizing: 'border-box',
  },
  btn: {
    padding: '10px 24px', background: '#6CB531',
    color: '#fff', border: 'none', borderRadius: 6,
    fontSize: 14, fontWeight: 600, cursor: 'pointer',
  },
  btnDisabled: { background: '#B1B1B1', cursor: 'not-allowed' },
}