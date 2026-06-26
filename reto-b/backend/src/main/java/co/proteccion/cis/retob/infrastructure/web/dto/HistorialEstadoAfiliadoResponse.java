package co.proteccion.cis.retob.infrastructure.web.dto;

import co.proteccion.cis.retob.domain.model.EstadoAfiliado;
import co.proteccion.cis.retob.domain.model.HistorialEstadoAfiliado;

import java.time.OffsetDateTime;

public record HistorialEstadoAfiliadoResponse(
        String id,
        String afiliadoId,
        EstadoAfiliado estadoAnterior,
        EstadoAfiliado estadoNuevo,
        OffsetDateTime cambiadoEn
) {
    public static HistorialEstadoAfiliadoResponse from(HistorialEstadoAfiliado h) {
        return new HistorialEstadoAfiliadoResponse(
                h.getId(), h.getAfiliadoId(),
                h.getEstadoAnterior(), h.getEstadoNuevo(),
                h.getCambiadoEn()
        );
    }
}
