package co.proteccion.cis.retob.domain.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Objeto de resultado: consolidado de aportes de un afiliado en un periodo.
 *
 * @param totalAportado   suma de los aportes APROBADOS (los que cuentan para el tope)
 * @param totalEnRevision suma de los aportes PENDIENTE_REVISION, informativo
 * @param detalle         todos los aportes del periodo, con su estado
 */
public record ConsolidadoAportes(
        String afiliadoId,
        String periodoDesde,
        String periodoHasta,
        BigDecimal totalAportado,
        BigDecimal totalEnRevision,
        List<Aporte> detalle
) {}
