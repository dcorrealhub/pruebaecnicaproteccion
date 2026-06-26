import { useState } from 'react'
import { registrarAporte } from '../api/aportesApi'
import Swal from 'sweetalert2'

function escaparHtml(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')
}

const LABEL_CANAL = { APP_MOVIL: 'App Móvil', WEB: 'Web', SUCURSAL: 'Sucursal' }

export default function RegistrarAporte() {
  const [form, setForm] = useState({ afiliadoId: '', monto: '', canal: 'APP_MOVIL' })
  const [resultado, setResultado] = useState(null)
  const [cargando, setCargando] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()

    const montoNum = parseFloat(form.monto)
    if (isNaN(montoNum) || montoNum <= 0) {
      Swal.fire({
        title: 'Monto Inválido',
        text: 'El monto ingresado debe ser mayor a cero.',
        icon: 'error',
        confirmButtonText: 'Aceptar',
        customClass: { popup: 'custom-swal-popup', title: 'custom-swal-title', confirmButton: 'custom-swal-confirm' }
      })
      return
    }

    // Confirmación — valores escapados para evitar XSS en el html: de Swal
    const confirmacion = await Swal.fire({
      title: '¿Confirmar Registro?',
      html: `
        <div style="text-align:left;font-size:0.95rem;">
          <p style="margin-bottom:0.5rem;">Estás a punto de registrar el siguiente aporte:</p>
          <ul style="list-style:none;padding-left:0;">
            <li style="margin-bottom:0.3rem;"><strong>ID Afiliado:</strong> ${escaparHtml(form.afiliadoId)}</li>
            <li style="margin-bottom:0.3rem;"><strong>Monto:</strong> $ ${montoNum.toLocaleString('es-CO', { minimumFractionDigits: 2 })} COP</li>
            <li><strong>Canal:</strong> ${escaparHtml(LABEL_CANAL[form.canal] ?? form.canal)}</li>
          </ul>
        </div>`,
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: 'Sí, registrar',
      cancelButtonText: 'Cancelar',
      customClass: { popup: 'custom-swal-popup', title: 'custom-swal-title', confirmButton: 'custom-swal-confirm', cancelButton: 'custom-swal-cancel' }
    })

    if (!confirmacion.isConfirmed) return

    setResultado(null)
    setCargando(true)

    try {
      const data = await registrarAporte({
        ...form,
        monto: montoNum,
        idempotenciaKey: crypto.randomUUID(),
      })

      setResultado(data)

      if (data.estado === 'EN_REVISION') {
        Swal.fire({
          title: 'Aporte en Revisión',
          text: `El aporte de $${montoNum.toLocaleString('es-CO')} ha sido guardado pero quedó en estado "EN_REVISION" y requiere aprobación manual.`,
          icon: 'warning',
          confirmButtonText: 'Entendido',
          customClass: { popup: 'custom-swal-popup', title: 'custom-swal-title', confirmButton: 'custom-swal-confirm' }
        })
      } else {
        Swal.fire({
          title: '¡Registro Exitoso!',
          text: `El aporte de $${montoNum.toLocaleString('es-CO')} fue procesado exitosamente.`,
          icon: 'success',
          confirmButtonText: 'Excelente',
          customClass: { popup: 'custom-swal-popup', title: 'custom-swal-title', confirmButton: 'custom-swal-confirm' }
        })
      }

      setForm({ afiliadoId: '', monto: '', canal: 'APP_MOVIL' })
    } catch (err) {
      Swal.fire({
        title: 'Error al Registrar',
        text: err.message || 'Ocurrió un error inesperado.',
        icon: 'error',
        confirmButtonText: 'Intentar de nuevo',
        customClass: { popup: 'custom-swal-popup', title: 'custom-swal-title', confirmButton: 'custom-swal-confirm' }
      })
    } finally {
      setCargando(false)
    }
  }

  return (
    <div className="card fade-in">
      <h2 className="card-title">Registrar Nuevo Aporte</h2>

      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem', maxWidth: '100%' }}>
        <div className="form-group">
          <label className="form-label">ID Afiliado (Sintético)</label>
          <input
            value={form.afiliadoId}
            onChange={e => setForm(f => ({ ...f, afiliadoId: e.target.value }))}
            placeholder="Ej: AF-001"
            className="form-input"
            required
            disabled={cargando}
          />
        </div>

        <div className="form-group">
          <label className="form-label">Monto (COP)</label>
          <input
            type="number"
            min="0.01"
            step="0.01"
            value={form.monto}
            onChange={e => setForm(f => ({ ...f, monto: e.target.value }))}
            placeholder="Ingrese el monto del aporte"
            className="form-input"
            required
            disabled={cargando}
          />
        </div>

        <div className="form-group">
          <label className="form-label">Canal de Recepción</label>
          <select
            value={form.canal}
            onChange={e => setForm(f => ({ ...f, canal: e.target.value }))}
            className="form-select"
            disabled={cargando}
          >
            <option value="APP_MOVIL">📲 App móvil</option>
            <option value="WEB">💻 Web</option>
            <option value="SUCURSAL">🏢 Sucursal</option>
          </select>
        </div>

        <button type="submit" className="btn btn-primary" disabled={cargando} style={{ marginTop: '0.5rem' }}>
          {cargando ? 'Registrando aporte...' : 'Registrar Aporte'}
        </button>
      </form>

      {resultado && (
        <div className="fade-in" style={{ marginTop: '1.5rem', padding: '1rem', border: '1px solid var(--border-light)', borderRadius: 'var(--radius-md)', backgroundColor: '#f8fafc' }}>
          <h4 style={{ fontSize: '0.9rem', marginBottom: '0.5rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span>Último aporte procesado:</span>
            <EstadoBadge estado={resultado.estado} />
          </h4>
          <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
            <strong>ID Transacción:</strong> <code style={{ fontSize: '0.8rem' }}>{resultado.id}</code>
          </p>
          {resultado.estado === 'EN_REVISION' && (
            <p style={{ fontSize: '0.8rem', color: 'var(--color-warning)', marginTop: '0.5rem', fontWeight: '500' }}>
              ⚠️ Este aporte ha sido retenido y requiere aprobación administrativa.
            </p>
          )}
        </div>
      )}
    </div>
  )
}

function EstadoBadge({ estado }) {
  const config = {
    PENDIENTE:   { cls: 'badge-success',  label: 'PENDIENTE' },
    EN_REVISION: { cls: 'badge-warning',  label: 'EN REVISIÓN' },
    APROBADO:    { cls: 'badge-success',  label: 'APROBADO' },
    RECHAZADO:   { cls: 'badge-danger',   label: 'RECHAZADO' },
  }
  const { cls, label } = config[estado] ?? { cls: 'badge-success', label: estado }
  return <span className={`badge ${cls}`}>{label}</span>
}
