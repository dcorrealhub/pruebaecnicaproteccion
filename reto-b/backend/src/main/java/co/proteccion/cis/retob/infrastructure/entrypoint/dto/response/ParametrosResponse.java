package co.proteccion.cis.retob.infrastructure.entrypoint.dto.response;

import java.math.BigDecimal;

public record ParametrosResponse(
        BigDecimal topeMensual,
        BigDecimal umbralRevision
) {
}
