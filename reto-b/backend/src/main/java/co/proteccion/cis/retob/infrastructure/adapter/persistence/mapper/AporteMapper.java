package co.proteccion.cis.retob.infrastructure.adapter.persistence.mapper;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.infrastructure.adapter.persistence.entity.AporteEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AporteMapper {

    public AporteEntity toEntity(Aporte aporte) {
        AporteEntity entity = new AporteEntity();
        entity.id(aporte.getId());
        entity.afiliadoId(aporte.getAfiliadoId());
        entity.monto(aporte.getMonto());
        entity.fecha(aporte.getFecha());
        entity.canal(aporte.getCanal());
        entity.periodo(aporte.getPeriodo());
        entity.marcadaRevision(aporte.isMarcadaRevision());
        entity.idempotenciaKey(aporte.getIdempotenciaKey());
        return entity;
    }

    public Aporte toDomain(AporteEntity entity) {
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
