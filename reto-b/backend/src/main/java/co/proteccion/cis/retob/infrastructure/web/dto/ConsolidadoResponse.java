package co.proteccion.cis.retob.infrastructure.web.dto;

import co.proteccion.cis.retob.domain.model.ConsolidadoAportes;

import java.math.BigDecimal;
import java.util.List;

public record ConsolidadoResponse(
        String afiliadoId,
        String periodoDesde,
        String periodoHasta,
        BigDecimal totalAportado,
        BigDecimal totalEnRevision,
        List<AporteResponse> detalle
) {
    public static ConsolidadoResponse from(ConsolidadoAportes consolidado) {
        return new ConsolidadoResponse(
                consolidado.afiliadoId(),
                consolidado.periodoDesde(),
                consolidado.periodoHasta(),
                consolidado.totalAportado(),
                consolidado.totalEnRevision(),
                consolidado.detalle().stream().map(AporteResponse::from).toList()
        );
    }
}
