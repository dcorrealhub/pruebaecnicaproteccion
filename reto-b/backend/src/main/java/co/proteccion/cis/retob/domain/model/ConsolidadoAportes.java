package co.proteccion.cis.retob.domain.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Objeto de resultado: consolidado de aportes de un afiliado en un periodo.
 */
public record ConsolidadoAportes(
        String afiliadoId,
        String periodoDesde,
        String periodoHasta,
        BigDecimal totalAportado,
        List<Aporte> detalle
) {}
