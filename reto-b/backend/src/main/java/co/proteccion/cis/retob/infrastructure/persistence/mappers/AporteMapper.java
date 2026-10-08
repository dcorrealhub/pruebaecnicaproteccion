package co.proteccion.cis.retob.infrastructure.persistence.mappers;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.infrastructure.persistence.entity.AporteEntity;

/**
 * Mapeo entre el modelo de dominio {@link Aporte} y la entidad JPA {@link AporteEntity}.
 */
public final class AporteMapper {

    private AporteMapper() {}

    public static AporteEntity toEntity(Aporte aporte) {
        return AporteEntity.builder()
                .id(aporte.getId())
                .afiliadoId(aporte.getAfiliadoId())
                .monto(aporte.getMonto())
                .fecha(aporte.getFecha())
                .canal(aporte.getCanal())
                .periodo(aporte.getPeriodo())
                .marcadaRevision(aporte.isMarcadaRevision())
                .idempotenciaKey(aporte.getIdempotenciaKey())
                .build();
    }

    public static Aporte toDomain(AporteEntity entity) {
        return new Aporte(
                entity.getId(),
                entity.getAfiliadoId(),
                entity.getMonto(),
                entity.getFecha(),
                entity.getCanal(),
                entity.getPeriodo(),
                entity.isMarcadaRevision(),
                entity.getIdempotenciaKey()
        );
    }
}
