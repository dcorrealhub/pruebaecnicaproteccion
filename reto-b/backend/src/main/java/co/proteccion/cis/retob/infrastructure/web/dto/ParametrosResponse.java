package co.proteccion.cis.retob.infrastructure.web.dto;

import co.proteccion.cis.retob.domain.model.ParametrosFondo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ParametrosResponse(
        String id,
        BigDecimal topeMensual,
        BigDecimal umbralRevision,
        String modificadoPor,
        OffsetDateTime modificadoEn,
        String comentario
) {
    public static ParametrosResponse from(ParametrosFondo p) {
        return new ParametrosResponse(p.getId(), p.getTopeMensual(), p.getUmbralRevision(),
                p.getModificadoPor(), p.getModificadoEn(), p.getComentario());
    }
}
