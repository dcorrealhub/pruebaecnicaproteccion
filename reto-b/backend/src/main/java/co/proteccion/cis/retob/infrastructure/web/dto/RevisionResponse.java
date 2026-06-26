package co.proteccion.cis.retob.infrastructure.web.dto;

import co.proteccion.cis.retob.domain.model.EstadoAporte;
import co.proteccion.cis.retob.domain.model.RevisionAporte;

import java.time.OffsetDateTime;

public record RevisionResponse(
        String id,
        String revisor,
        EstadoAporte decision,
        String comentario,
        OffsetDateTime ocurridoEn
) {
    public static RevisionResponse from(RevisionAporte r) {
        return new RevisionResponse(r.getId(), r.getRevisor(), r.getDecision(), r.getComentario(), r.getOcurridoEn());
    }
}
