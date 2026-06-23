package co.proteccion.cis.retob.infrastructure.persistence.mapper;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.CanalAporte;
import co.proteccion.cis.retob.domain.model.EstadoAporte;
import co.proteccion.cis.retob.infrastructure.persistence.entity.AporteEntity;
import org.springframework.stereotype.Component;

@Component
public class AportePersistenceMapper {

    public Aporte toDomain(AporteEntity entity) {
        return new Aporte(
                entity.getId(),
                entity.getAfiliadoId(),
                entity.getMonto(),
                entity.getFecha(),
                toDomainCanal(entity.getCanal()),
                entity.getPeriodo(),
                toDomainEstado(entity.getEstado()),
                entity.getIdempotenciaKey()
        );
    }

    public AporteEntity toEntity(Aporte aporte) {
        return AporteEntity.builder()
                .id(aporte.getId())
                .afiliadoId(aporte.getAfiliadoId())
                .monto(aporte.getMonto())
                .fecha(aporte.getFecha())
                .canal(toPersistenceCanal(aporte.getCanal()))
                .periodo(aporte.getPeriodo())
                .estado(toPersistenceEstado(aporte.getEstado()))
                .idempotenciaKey(aporte.getIdempotenciaKey())
                .build();
    }

    private CanalAporte toDomainCanal(
            co.proteccion.cis.retob.infrastructure.persistence.enums.CanalAporte canal) {
        return CanalAporte.valueOf(canal.name());
    }

    private co.proteccion.cis.retob.infrastructure.persistence.enums.CanalAporte toPersistenceCanal(
            CanalAporte canal) {
        return co.proteccion.cis.retob.infrastructure.persistence.enums.CanalAporte.valueOf(canal.name());
    }

    private EstadoAporte toDomainEstado(
            co.proteccion.cis.retob.infrastructure.persistence.enums.EstadoAporte estado) {
        return EstadoAporte.valueOf(estado.name());
    }

    private co.proteccion.cis.retob.infrastructure.persistence.enums.EstadoAporte toPersistenceEstado(
            EstadoAporte estado) {
        return co.proteccion.cis.retob.infrastructure.persistence.enums.EstadoAporte.valueOf(estado.name());
    }
}
