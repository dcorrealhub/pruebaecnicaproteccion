package co.proteccion.cis.retob.infrastructure.persistence.mappers;

import co.proteccion.cis.retob.domain.model.EventoAporte;
import co.proteccion.cis.retob.infrastructure.persistence.entity.EventoAporteEntity;

/**
 * Mapeo entre el modelo de dominio {@link EventoAporte} y la entidad JPA {@link EventoAporteEntity}.
 */
public final class EventoAporteMapper {

    private EventoAporteMapper() {}

    public static EventoAporteEntity toEntity(EventoAporte evento) {
        return EventoAporteEntity.builder()
                .id(evento.getId())
                .aporteId(evento.getAporteId())
                .tipo(evento.getTipo())
                .ocurridoEn(evento.getOcurridoEn())
                .build();
    }

    public static EventoAporte toDomain(EventoAporteEntity entity) {
        return new EventoAporte(
                entity.getId(),
                entity.getAporteId(),
                entity.getTipo(),
                entity.getOcurridoEn()
        );
    }
}
