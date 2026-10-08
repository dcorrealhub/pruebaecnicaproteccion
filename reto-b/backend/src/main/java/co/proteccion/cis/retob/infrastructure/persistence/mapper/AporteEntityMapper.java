package co.proteccion.cis.retob.infrastructure.persistence.mapper;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.Canal;
import co.proteccion.cis.retob.infrastructure.persistence.entity.AporteEntity;

/**
 * Mapeo entre la entidad JPA {@link AporteEntity} y el modelo de dominio {@link Aporte}.
 * El canal se persiste como su nombre (String) y se reconstruye como enum.
 */
public final class AporteEntityMapper {

    private AporteEntityMapper() {
    }

    public static AporteEntity aEntidad(Aporte aporte) {
        return new AporteEntity(
                aporte.getId(),
                aporte.getAfiliadoId(),
                aporte.getMonto(),
                aporte.getFecha(),
                aporte.getCanal().name(),
                aporte.getPeriodo(),
                aporte.isMarcadaRevision(),
                aporte.getIdempotenciaKey(),
                null);
    }

    public static Aporte aDominio(AporteEntity entity) {
        return new Aporte(
                entity.getId(),
                entity.getAfiliadoId(),
                entity.getMonto(),
                entity.getFecha(),
                Canal.valueOf(entity.getCanal()),
                entity.getPeriodo(),
                entity.isMarcadaRevision(),
                entity.getIdempotenciaKey());
    }
}
