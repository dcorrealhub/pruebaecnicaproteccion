package co.proteccion.cis.retob.infrastructure.entrypoint.dto.response;

import co.proteccion.cis.retob.domain.model.aporte.EstadoAporte;

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
}
