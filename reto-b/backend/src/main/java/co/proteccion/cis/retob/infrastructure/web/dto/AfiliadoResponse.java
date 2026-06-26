package co.proteccion.cis.retob.infrastructure.web.dto;

import co.proteccion.cis.retob.domain.model.Afiliado;
import co.proteccion.cis.retob.domain.model.EstadoAfiliado;

import java.time.OffsetDateTime;

public record AfiliadoResponse(
        String id,
        String afiliadoId,
        String nombre,
        EstadoAfiliado estado,
        OffsetDateTime creadoEn
) {
    public static AfiliadoResponse from(Afiliado a) {
        return new AfiliadoResponse(a.getId(), a.getAfiliadoId(), a.getNombre(), a.getEstado(), a.getCreadoEn());
    }
}
