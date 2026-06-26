package co.proteccion.cis.retob.infrastructure.web.dto;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.EstadoAporte;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AporteResponse(
        Long id,
        String afiliadoId,
        BigDecimal monto,
        LocalDate fecha,
        String canal,
        String periodo,
        EstadoAporte estado,
        boolean marcadaRevision
) {
    public static AporteResponse from(Aporte aporte) {
        return new AporteResponse(
                aporte.getId(),
                aporte.getAfiliadoId(),
                aporte.getMonto(),
                aporte.getFecha(),
                aporte.getCanal(),
                aporte.getPeriodo(),
                aporte.getEstado(),
                aporte.isMarcadaRevision()
        );
    }
}
