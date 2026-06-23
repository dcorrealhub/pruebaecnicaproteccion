package co.proteccion.cis.retob.infrastructure.web.dto;

import co.proteccion.cis.retob.domain.model.ConsolidadoAportes;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public record ConsolidadoResponse(
        String afiliadoId,
        String periodoDesde,
        String periodoHasta,
        BigDecimal totalAportado,
        List<AporteResponse> detalle
) {
    public static ConsolidadoResponse from(ConsolidadoAportes consolidado) {
        return new ConsolidadoResponse(
                consolidado.afiliadoId(),
                consolidado.periodoDesde(),
                consolidado.periodoHasta(),
                consolidado.totalAportado(),
                consolidado.detalle().stream().map(AporteResponse::from).collect(Collectors.toList())
        );
    }
}
