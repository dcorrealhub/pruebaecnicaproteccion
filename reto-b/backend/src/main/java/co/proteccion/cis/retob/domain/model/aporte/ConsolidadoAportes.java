package co.proteccion.cis.retob.domain.model.aporte;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado de consulta: consolidado de aportes de un afiliado en un periodo.
 *
 * @param totalAportado   suma de aportes APROBADOS
 * @param totalEnRevision suma de aportes PENDIENTE_REVISION
 * @param detalle         todos los aportes del rango, con su estado
 */
public record ConsolidadoAportes(
        String afiliadoId,
        String periodoDesde,
        String periodoHasta,
        BigDecimal totalAportado,
        BigDecimal totalEnRevision,
        List<Aporte> detalle
) {
}
