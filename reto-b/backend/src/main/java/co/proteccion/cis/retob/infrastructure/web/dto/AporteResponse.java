package co.proteccion.cis.retob.infrastructure.web.dto;

import co.proteccion.cis.retob.domain.model.Aporte;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AporteResponse(
        Long id,
        String afiliadoId,
        BigDecimal monto,
        LocalDate fecha,
        String canal,
        String periodo,
        String estado
) {
    public static AporteResponse from(Aporte aporte) {
        return new AporteResponse(
                aporte.getId(),
                aporte.getAfiliadoId(),
                aporte.getMonto(),
                aporte.getFecha(),
                aporte.getCanal().name(),
                aporte.getPeriodo(),
                aporte.getEstado().name()
        );
    }
}
