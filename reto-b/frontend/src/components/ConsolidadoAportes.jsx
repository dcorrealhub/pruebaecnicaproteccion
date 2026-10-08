import { useState } from 'react'
import { consultarConsolidado } from '../api/aportesApi'

const FMT_COP = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP' })

export default function ConsolidadoAportes() {
  const [filtros, setFiltros] = useState({ afiliadoId: '', periodoDesde: '', periodoHasta: '' })
  const [consolidado, setConsolidado] = useState(null)
  const [error, setError] = useState(null)
  const [cargando, setCargando] = useState(false)

  function actualizar(campo, valor) {
    setFiltros(f => ({ ...f, [campo]: valor }))
  }

  async function handleBuscar(e) {
    e.preventDefault()
    setError(null)
    setConsolidado(null)
    setCargando(true)

    try {
      const data = await consultarConsolidado(filtros)
      setConsolidado(data)
    } catch (err) {
      setError(err.message)
    } finally {
      setCargando(false)
    }
  }

  return (
    <section className="card">
      <h2 className="card__titulo">Consolidado de aportes</h2>

      <form onSubmit={handleBuscar} className="form form--fila">
        <div className="campo">
          <label className="campo__label" htmlFor="f-afiliado">ID Afiliado</label>
          <input
            id="f-afiliado"
            className="input"
            value={filtros.afiliadoId}
            onChange={e => actualizar('afiliadoId', e.target.value)}
            placeholder="AF-001"
            required
          />
        </div>

        <div className="campo">
          <label className="campo__label" htmlFor="f-desde">Periodo desde (YYYY-MM)</label>
          <input
            id="f-desde"
            className="input"
            value={filtros.periodoDesde}
            onChange={e => actualizar('periodoDesde', e.target.value)}
            placeholder="2025-01"
            pattern="\d{4}-\d{2}"
            required
          />
        </div>

        <div className="campo">
          <label className="campo__label" htmlFor="f-hasta">Periodo hasta (YYYY-MM)</label>
          <input
            id="f-hasta"
            className="input"
            value={filtros.periodoHasta}
            onChange={e => actualizar('periodoHasta', e.target.value)}
            placeholder="2025-06"
            pattern="\d{4}-\d{2}"
            required
          />
        </div>

        <button type="submit" className="btn btn--primario" disabled={cargando}>
          {cargando ? 'Consultando...' : 'Consultar'}
        </button>
      </form>

      {error && <p className="alerta alerta--error" role="alert">Error: {error}</p>}

      {consolidado && (
        <div style={{ marginTop: 20 }}>
          <p className="resumen-total">
            <strong>Total aportado:</strong> {FMT_COP.format(consolidado.totalAportado ?? 0)}
          </p>

          {consolidado.detalle?.length > 0 ? (
            <table className="tabla">
              <thead>
                <tr>
                  <th>Fecha</th>
                  <th className="num">Monto</th>
                  <th>Canal</th>
                  <th>Revisión</th>
                </tr>
              </thead>
              <tbody>
                {consolidado.detalle.map(a => (
                  <tr key={a.id}>
                    <td>{a.fecha}</td>
                    <td className="num">{FMT_COP.format(a.monto ?? 0)}</td>
                    <td>{a.canal}</td>
                    <td>
                      <span className={`chip ${a.marcadaRevision ? 'chip--si' : 'chip--no'}`}>
                        {a.marcadaRevision ? 'Sí' : 'No'}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <p className="vacio">No se encontraron aportes en el periodo indicado.</p>
          )}
        </div>
      )}
    </section>
  )
}
