package co.proteccion.cis.retob.domain.model.aporte;

/**
 * Parámetros de consulta del consolidado (periodos en formato YYYY-MM).
 */
public record ConsultaConsolidado(
        String afiliadoId,
        String periodoDesde,
        String periodoHasta
) {
}
