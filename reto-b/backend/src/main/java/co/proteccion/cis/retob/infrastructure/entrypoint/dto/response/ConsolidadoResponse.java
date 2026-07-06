package co.proteccion.cis.retob.infrastructure.entrypoint.dto.response;

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
}
