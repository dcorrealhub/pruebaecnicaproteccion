import { useCallback, useEffect, useState } from 'react'
import { consultarConsolidado } from '../api/aportesApi'
import MensajeError from './MensajeError'
import { CANALES, formatoCOP, formatoFecha, formatoPeriodo } from '../formato'

// yyyy-MM en hora local (toISOString usaría UTC y podría dar el mes siguiente)
const mesActual = () => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}

/**
 * Consolidado de aportes de un afiliado en un periodo (yyyy-MM): total + detalle.
 *
 * @param {{ ultimoRegistro?: { afiliadoId: string, seq: number } | null }} props
 *   Cuando cambia y su afiliadoId coincide con la consulta mostrada, se vuelve a consultar.
 */
export default function ConsolidadoAportes({ ultimoRegistro }) {
  const [filtros, setFiltros] = useState({ afiliadoId: '', periodo: mesActual() })
  const [consultaActual, setConsultaActual] = useState(null) // filtros de la última consulta exitosa
  const [consolidado, setConsolidado] = useState(null)
  const [error, setError] = useState(null)
  const [cargando, setCargando] = useState(false)

  const consultar = useCallback(async params => {
    setError(null)
    setCargando(true)
    try {
      const data = await consultarConsolidado(params)
      setConsolidado(data)
      setConsultaActual(params)
    } catch (err) {
      setConsolidado(null)
      setError(err)
    } finally {
      setCargando(false)
    }
  }, [])

  // Refresco automático tras registrar un aporte del mismo afiliado
  useEffect(() => {
    if (ultimoRegistro && consultaActual && ultimoRegistro.afiliadoId === consultaActual.afiliadoId) {
      consultar(consultaActual)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- solo debe reaccionar a nuevos registros
  }, [ultimoRegistro])

  function handleBuscar(e) {
    e.preventDefault()
    consultar({ afiliadoId: filtros.afiliadoId.trim(), periodo: filtros.periodo.trim() })
  }

  const detalle = consolidado?.detalle ?? []
  const enRevision = detalle.filter(a => a.marcadaRevision).length

  return (
    <section className="card">
      <h2 className="card__title">Consolidado de aportes</h2>
      <p className="card__subtitle">Total aportado y detalle de un afiliado en un periodo.</p>

      <form onSubmit={handleBuscar} className="form-grid">
        <label className="field">
          <span className="field__label">ID Afiliado</span>
          <input
            className="input"
            value={filtros.afiliadoId}
            onChange={e => setFiltros(f => ({ ...f, afiliadoId: e.target.value }))}
            placeholder="AF-001"
            required
          />
        </label>

        <label className="field">
          <span className="field__label">Periodo (yyyy-MM)</span>
          <input
            className="input"
            value={filtros.periodo}
            onChange={e => setFiltros(f => ({ ...f, periodo: e.target.value }))}
            placeholder="2026-10"
            pattern="\d{4}-(0[1-9]|1[0-2])"
            title="Formato yyyy-MM, p. ej. 2026-10"
            required
          />
        </label>

        <button type="submit" className="btn" disabled={cargando}>
          {cargando ? 'Consultando…' : 'Consultar'}
        </button>
      </form>

      <MensajeError error={error} />

      {consolidado && (
        <>
          <div className="stats">
            <div className="stat stat--primary">
              <div className="stat__label">Total {formatoPeriodo(consolidado.periodoDesde)}</div>
              <div className="stat__value">{formatoCOP(consolidado.totalAportado)}</div>
            </div>
            <div className="stat">
              <div className="stat__label">Afiliado</div>
              <div className="stat__value">{consolidado.afiliadoId}</div>
            </div>
            <div className="stat">
              <div className="stat__label">Aportes</div>
              <div className="stat__value">{detalle.length}</div>
            </div>
            <div className="stat">
              <div className="stat__label">En revisión</div>
              <div className="stat__value">{enRevision}</div>
            </div>
          </div>

          {detalle.length > 0 ? (
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Fecha</th>
                    <th className="num">Monto</th>
                    <th>Canal</th>
                    <th>Revisión</th>
                  </tr>
                </thead>
                <tbody>
                  {detalle.map(a => (
                    <tr key={a.id}>
                      <td>{formatoFecha(a.fecha)}</td>
                      <td className="num">{formatoCOP(a.monto)}</td>
                      <td><span className="badge badge--neutral">{CANALES[a.canal] ?? a.canal}</span></td>
                      <td>
                        {a.marcadaRevision
                          ? <span className="badge badge--warning">Marcado para revisión</span>
                          : <span className="badge badge--ok">Sin revisión</span>}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="empty">No se encontraron aportes en el periodo indicado.</div>
          )}
        </>
      )}
    </section>
  )
}
