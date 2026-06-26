import { useState } from 'react'
import { consultarConsolidado, cambiarEstadoAporte, consultarRevisiones } from '../api/aportesApi'
import Swal from 'sweetalert2'

const BADGE_ESTADO = {
  PENDIENTE:   { cls: 'badge-warning', label: 'Pendiente' },
  EN_REVISION: { cls: 'badge-warning', label: 'En revisión' },
  APROBADO:    { cls: 'badge-success', label: 'Aprobado' },
  RECHAZADO:   { cls: 'badge-danger',  label: 'Rechazado' },
}

const LABEL_CANAL = { APP_MOVIL: '📲 App móvil', WEB: '💻 Web', SUCURSAL: '🏢 Sucursal' }

// Transiciones válidas según dominio
const ACCIONES_POR_ESTADO = {
  PENDIENTE:   [
    { nuevoEstado: 'EN_REVISION', label: '🔍 Enviar a revisión', cls: 'btn-warning' },
    { nuevoEstado: 'APROBADO',    label: '✅ Aprobar',           cls: 'btn-success' },
  ],
  EN_REVISION: [
    { nuevoEstado: 'APROBADO',    label: '✅ Aprobar',           cls: 'btn-success' },
    { nuevoEstado: 'RECHAZADO',   label: '🚫 Rechazar',          cls: 'btn-danger'  },
  ],
  APROBADO:    [],
  RECHAZADO:   [],
}

function escaparHtml(str) {
  return String(str ?? '')
    .replace(/&/g, '&amp;').replace(/</g, '&lt;')
    .replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#039;')
}

export default function GestionAportes() {
  const [filtros, setFiltros] = useState({ afiliadoId: '', periodoDesde: '', periodoHasta: '' })
  const [aportes, setAportes] = useState([])
  const [cargando, setCargando]   = useState(false)
  const [accionando, setAccionando] = useState(null)  // id del aporte en proceso

  async function handleBuscar(e) {
    e.preventDefault()
    setAportes([])
    setCargando(true)
    try {
      const data = await consultarConsolidado(filtros)
      setAportes(data.detalle ?? [])
      if (!data.detalle || data.detalle.length === 0) {
        Swal.fire({
          title: 'Sin resultados',
          text: 'No se encontraron aportes para ese afiliado en el periodo indicado.',
          icon: 'info',
          confirmButtonText: 'Aceptar',
          customClass: { popup: 'custom-swal-popup', title: 'custom-swal-title', confirmButton: 'custom-swal-confirm' },
        })
      }
    } catch (err) {
      Swal.fire({
        title: 'Error de consulta',
        text: err.message,
        icon: 'error',
        confirmButtonText: 'Aceptar',
        customClass: { popup: 'custom-swal-popup', title: 'custom-swal-title', confirmButton: 'custom-swal-confirm' },
      })
    } finally {
      setCargando(false)
    }
  }

  async function handleAccion(aporte, nuevoEstado, labelAccion) {
    const { value: datos } = await Swal.fire({
      title: labelAccion.replace(/^[^ ]+ /, ''),
      html: `
        <p style="text-align:left;margin-bottom:0.75rem;font-size:0.9rem;color:#64748b;">
          Aporte <strong>${aporte.id.substring(0, 8)}…</strong> &nbsp;|&nbsp;
          $ ${Number(aporte.monto).toLocaleString('es-CO', { minimumFractionDigits: 2 })} COP
        </p>
        <input  id="swal-revisor"
                class="swal2-input"
                placeholder="Nombre del revisor *"
                style="margin-bottom:0.75rem;" />
        <textarea id="swal-comentario"
                  class="swal2-textarea"
                  placeholder="Comentario (opcional)"
                  rows="3"
                  style="resize:vertical;"></textarea>`,
      showCancelButton: true,
      confirmButtonText: 'Confirmar',
      cancelButtonText: 'Cancelar',
      customClass: {
        popup: 'custom-swal-popup',
        title: 'custom-swal-title',
        confirmButton: 'custom-swal-confirm',
        cancelButton: 'custom-swal-cancel',
      },
      focusConfirm: false,
      preConfirm: () => {
        const revisor = document.getElementById('swal-revisor').value.trim()
        if (!revisor) {
          Swal.showValidationMessage('El nombre del revisor es obligatorio')
          return false
        }
        return {
          revisor,
          comentario: document.getElementById('swal-comentario').value.trim() || null,
        }
      },
    })

    if (!datos) return

    setAccionando(aporte.id)
    try {
      const actualizado = await cambiarEstadoAporte(aporte.id, {
        nuevoEstado,
        revisor: datos.revisor,
        comentario: datos.comentario,
      })

      setAportes(prev => prev.map(a => a.id === aporte.id ? { ...a, estado: actualizado.estado } : a))

      Swal.fire({
        title: '¡Listo!',
        text: `El aporte pasó a estado ${actualizado.estado}.`,
        icon: 'success',
        confirmButtonText: 'Aceptar',
        customClass: { popup: 'custom-swal-popup', title: 'custom-swal-title', confirmButton: 'custom-swal-confirm' },
      })
    } catch (err) {
      Swal.fire({
        title: 'Error',
        text: err.message,
        icon: 'error',
        confirmButtonText: 'Aceptar',
        customClass: { popup: 'custom-swal-popup', title: 'custom-swal-title', confirmButton: 'custom-swal-confirm' },
      })
    } finally {
      setAccionando(null)
    }
  }

  async function handleVerRevisiones(aporte) {
    try {
      const revisiones = await consultarRevisiones(aporte.id)
      if (revisiones.length === 0) {
        Swal.fire({
          title: 'Sin revisiones',
          text: 'Este aporte aún no tiene revisiones registradas.',
          icon: 'info',
          confirmButtonText: 'Cerrar',
          customClass: { popup: 'custom-swal-popup', title: 'custom-swal-title', confirmButton: 'custom-swal-confirm' },
        })
        return
      }

      const filas = revisiones.map(r => {
        const badge = BADGE_ESTADO[r.decision] ?? { cls: 'badge-success', label: r.decision }
        const fecha = new Date(r.ocurridoEn).toLocaleString('es-CO', {
          dateStyle: 'short', timeStyle: 'short'
        })
        return `
          <tr style="border-bottom:1px solid #e2e8f0;">
            <td style="padding:0.5rem 0.75rem;font-size:0.82rem;">${escaparHtml(fecha)}</td>
            <td style="padding:0.5rem 0.75rem;font-size:0.82rem;font-weight:600;">${escaparHtml(r.revisor)}</td>
            <td style="padding:0.5rem 0.75rem;">
              <span style="display:inline-flex;align-items:center;padding:0.2rem 0.6rem;font-size:0.72rem;font-weight:600;border-radius:9999px;text-transform:uppercase;letter-spacing:0.03em;"
                    class="badge ${badge.cls}">${escaparHtml(badge.label)}</span>
            </td>
            <td style="padding:0.5rem 0.75rem;font-size:0.82rem;color:#64748b;">${escaparHtml(r.comentario ?? '—')}</td>
          </tr>`
      }).join('')

      Swal.fire({
        title: `Historial de revisiones`,
        html: `
          <div style="overflow-x:auto;max-height:340px;overflow-y:auto;">
            <table style="width:100%;border-collapse:collapse;text-align:left;">
              <thead>
                <tr style="background:#f8fafc;border-bottom:2px solid #e2e8f0;">
                  <th style="padding:0.5rem 0.75rem;font-size:0.78rem;color:#64748b;">Fecha</th>
                  <th style="padding:0.5rem 0.75rem;font-size:0.78rem;color:#64748b;">Revisor</th>
                  <th style="padding:0.5rem 0.75rem;font-size:0.78rem;color:#64748b;">Decisión</th>
                  <th style="padding:0.5rem 0.75rem;font-size:0.78rem;color:#64748b;">Comentario</th>
                </tr>
              </thead>
              <tbody>${filas}</tbody>
            </table>
          </div>`,
        confirmButtonText: 'Cerrar',
        width: '680px',
        customClass: { popup: 'custom-swal-popup', title: 'custom-swal-title', confirmButton: 'custom-swal-confirm' },
      })
    } catch (err) {
      Swal.fire({
        title: 'Error',
        text: err.message,
        icon: 'error',
        confirmButtonText: 'Aceptar',
        customClass: { popup: 'custom-swal-popup', title: 'custom-swal-title', confirmButton: 'custom-swal-confirm' },
      })
    }
  }

  return (
    <div className="card fade-in" style={{ width: '100%' }}>
      <h2 className="card-title">Gestión y Revisión de Aportes</h2>

      <form onSubmit={handleBuscar} style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
        gap: '1rem',
        alignItems: 'flex-end',
        marginBottom: '2rem',
        padding: '1.25rem',
        backgroundColor: '#f8fafc',
        borderRadius: 'var(--radius-md)',
        border: '1px solid var(--border-light)',
      }}>
        <div className="form-group" style={{ marginBottom: 0 }}>
          <label className="form-label">ID Afiliado</label>
          <input
            value={filtros.afiliadoId}
            onChange={e => setFiltros(f => ({ ...f, afiliadoId: e.target.value }))}
            placeholder="Ej: AF-001"
            className="form-input"
            required
            disabled={cargando}
          />
        </div>
        <div className="form-group" style={{ marginBottom: 0 }}>
          <label className="form-label">Periodo Desde (YYYY-MM)</label>
          <input
            value={filtros.periodoDesde}
            onChange={e => setFiltros(f => ({ ...f, periodoDesde: e.target.value }))}
            placeholder="2026-01"
            pattern="\d{4}-\d{2}"
            className="form-input"
            required
            disabled={cargando}
          />
        </div>
        <div className="form-group" style={{ marginBottom: 0 }}>
          <label className="form-label">Periodo Hasta (YYYY-MM)</label>
          <input
            value={filtros.periodoHasta}
            onChange={e => setFiltros(f => ({ ...f, periodoHasta: e.target.value }))}
            placeholder="2026-06"
            pattern="\d{4}-\d{2}"
            className="form-input"
            required
            disabled={cargando}
          />
        </div>
        <button type="submit" className="btn btn-primary" disabled={cargando}>
          {cargando ? 'Buscando...' : '🔍 Buscar'}
        </button>
      </form>

      {aportes.length > 0 && (
        <div className="fade-in">
          <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '1rem' }}>
            {aportes.length} aporte{aportes.length !== 1 ? 's' : ''} encontrado{aportes.length !== 1 ? 's' : ''}
          </p>
          <div className="table-container" style={{ overflowX: 'auto' }}>
            <table className="custom-table">
              <thead>
                <tr>
                  <th>Fecha</th>
                  <th>Monto</th>
                  <th>Canal</th>
                  <th>Estado</th>
                  <th style={{ textAlign: 'center' }}>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {aportes.map(aporte => {
                  const badge    = BADGE_ESTADO[aporte.estado] ?? { cls: 'badge-success', label: aporte.estado }
                  const acciones = ACCIONES_POR_ESTADO[aporte.estado] ?? []
                  const enProceso = accionando === aporte.id

                  return (
                    <tr key={aporte.id}>
                      <td style={{ fontWeight: 500, whiteSpace: 'nowrap' }}>{aporte.fecha}</td>
                      <td style={{ fontFamily: 'monospace', fontWeight: 600, whiteSpace: 'nowrap' }}>
                        $ {Number(aporte.monto).toLocaleString('es-CO', { minimumFractionDigits: 2 })}
                      </td>
                      <td>{LABEL_CANAL[aporte.canal] ?? aporte.canal}</td>
                      <td>
                        <span className={`badge ${badge.cls}`}>{badge.label}</span>
                      </td>
                      <td>
                        <div style={{ display: 'flex', gap: '0.4rem', justifyContent: 'center', flexWrap: 'wrap' }}>
                          {acciones.map(({ nuevoEstado, label, cls }) => (
                            <button
                              key={nuevoEstado}
                              className={`btn btn-sm ${cls}`}
                              disabled={enProceso}
                              onClick={() => handleAccion(aporte, nuevoEstado, label)}
                            >
                              {enProceso ? '...' : label}
                            </button>
                          ))}
                          <button
                            className="btn btn-sm btn-outline"
                            disabled={enProceso}
                            onClick={() => handleVerRevisiones(aporte)}
                          >
                            📋 Historial
                          </button>
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  )
}
