import { useState, useCallback } from 'react'
import { consultarConsolidado } from '../api/aportesApi'
import styles from '../styles/ConsolidadoAportes.module.css'

const PERIODO_REGEX = /^\d{4}-(0[1-9]|1[0-2])$/

function validarFiltros({ afiliadoId, periodoDesde, periodoHasta }) {
  const errores = {}
  if (!afiliadoId.trim()) {
    errores.afiliadoId = 'El ID del afiliado es obligatorio.'
  }
  if (!periodoDesde.trim()) {
    errores.periodoDesde = 'El periodo desde es obligatorio.'
  } else if (!PERIODO_REGEX.test(periodoDesde)) {
    errores.periodoDesde = 'Formato inválido. Use YYYY-MM (ej: 2025-01).'
  }
  if (!periodoHasta.trim()) {
    errores.periodoHasta = 'El periodo hasta es obligatorio.'
  } else if (!PERIODO_REGEX.test(periodoHasta)) {
    errores.periodoHasta = 'Formato inválido. Use YYYY-MM (ej: 2025-06).'
  }
  return errores
}

export default function ConsolidadoAportes() {
  const [filtros, setFiltros] = useState({ afiliadoId: '', periodoDesde: '', periodoHasta: '' })
  const [consolidado, setConsolidado] = useState(null)
  const [error, setError] = useState(null)
  const [cargando, setCargando] = useState(false)
  const [errores, setErrores] = useState({})
  const [consultado, setConsultado] = useState(false)

  const actualizarCampo = useCallback((campo, valor) => {
    setFiltros(f => ({ ...f, [campo]: valor }))
    setErrores(e => {
      if (!e[campo]) return e
      const next = { ...e }
      delete next[campo]
      return next
    })
  }, [])

  function handleBuscar(e) {
    e.preventDefault()
    setError(null)
    setConsolidado(null)
    setConsultado(false)

    const v = validarFiltros(filtros)
    setErrores(v)
    if (Object.keys(v).length > 0) return

    setCargando(true)

    consultarConsolidado(filtros)
      .then(data => {
        setConsolidado(data)
        setConsultado(true)
      })
      .catch(err => setError(err.message))
      .finally(() => setCargando(false))
  }

  return (
    <div>
      <h2 className={styles.title}>Consolidado de aportes</h2>

      <form onSubmit={handleBuscar} className={styles.searchForm}>
        <label className={styles.label}>
          ID Afiliado
          <input
            value={filtros.afiliadoId}
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
          Periodo desde
          <input
            value={filtros.periodoDesde}
            onChange={e => actualizarCampo('periodoDesde', e.target.value)}
            placeholder="2025-01"
            pattern="\d{4}-\d{2}"
            required
            className={styles.input}
          />
          {errores.periodoDesde && (
            <span role="alert" className={styles.fieldError}>{errores.periodoDesde}</span>
          )}
        </label>

        <label className={styles.label}>
          Periodo hasta
          <input
            value={filtros.periodoHasta}
            onChange={e => actualizarCampo('periodoHasta', e.target.value)}
            placeholder="2025-06"
            pattern="\d{4}-\d{2}"
            required
            className={styles.input}
          />
          {errores.periodoHasta && (
            <span role="alert" className={styles.fieldError}>{errores.periodoHasta}</span>
          )}
        </label>

        <button type="submit" disabled={cargando} className={styles.submitButton}>
          {cargando ? 'Consultando...' : 'Consultar'}
        </button>
      </form>

      {error && (
        <p role="alert" className={styles.serverError}>{error}</p>
      )}

      {consultado && consolidado && (
        <div role="region" aria-label="Resultado del consolidado" className={styles.resultSection}>
          <p className={styles.totalRow}>
            <strong>Total aportado:</strong>{' '}
            {consolidado.totalAportado?.toLocaleString('es-CO', { style: 'currency', currency: 'COP' })}
          </p>

          {consolidado.detalle?.length > 0 ? (
            <table className={styles.table}>
              <caption className={styles.caption}>
                {consolidado.detalle.length} aporte{consolidado.detalle.length !== 1 ? 's' : ''} encontrado{consolidado.detalle.length !== 1 ? 's' : ''}
              </caption>
              <thead>
                <tr>
                  <th>Fecha</th>
                  <th>Monto</th>
                  <th>Canal</th>
                  <th>Revisión</th>
                </tr>
              </thead>
              <tbody>
                {consolidado.detalle.map(a => (
                  <tr key={a.id}>
                    <td>{a.fecha}</td>
                    <td>{a.monto?.toLocaleString('es-CO', { style: 'currency', currency: 'COP' })}</td>
                    <td>{a.canal}</td>
                    <td>{a.marcadaRevision ? 'Sí' : 'No'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <p className={styles.emptyResult}>No se encontraron aportes en el periodo indicado.</p>
          )}
        </div>
      )}
    </div>
  )
}
