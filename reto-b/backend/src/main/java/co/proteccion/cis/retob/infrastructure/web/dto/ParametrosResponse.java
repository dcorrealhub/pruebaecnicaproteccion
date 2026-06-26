package co.proteccion.cis.retob.infrastructure.web.dto;

import co.proteccion.cis.retob.domain.model.ParametrosAporte;

import java.math.BigDecimal;

public record ParametrosResponse(
        BigDecimal topeMensual,
        BigDecimal umbralRevision
) {
    public static ParametrosResponse from(ParametrosAporte p) {
        return new ParametrosResponse(p.topeMensual(), p.umbralRevision());
    }
}
