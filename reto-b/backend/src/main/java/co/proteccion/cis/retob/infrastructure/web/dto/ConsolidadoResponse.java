package co.proteccion.cis.retob.infrastructure.web.dto;

import co.proteccion.cis.retob.domain.model.ConsolidadoAportes;

import java.math.BigDecimal;
import java.util.List;

public record ConsolidadoResponse(
        String afiliadoId,
        PeriodoResponse periodo,
        ResumenResponse resumen,
        List<AporteResponse> aportes
) {

    public static ConsolidadoResponse from(ConsolidadoAportes consolidado) {
        List<AporteResponse> items = consolidado.detalle().stream()
                .map(AporteResponse::from)
                .toList();

        return new ConsolidadoResponse(
                consolidado.afiliadoId(),
                new PeriodoResponse(consolidado.periodoDesde(), consolidado.periodoHasta()),
                new ResumenResponse(consolidado.totalAportado(), items.size()),
                items
        );
    }

    public record PeriodoResponse(String desde, String hasta) {}

    public record ResumenResponse(BigDecimal totalAportado, int cantidadAportes) {}
}
