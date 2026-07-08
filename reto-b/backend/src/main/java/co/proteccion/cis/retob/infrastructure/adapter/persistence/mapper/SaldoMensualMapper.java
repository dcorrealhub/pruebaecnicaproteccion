package co.proteccion.cis.retob.infrastructure.adapter.persistence.mapper;

import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.infrastructure.adapter.persistence.entity.SaldoMensualEntity;
import org.springframework.stereotype.Component;

@Component
public class SaldoMensualMapper {

    public SaldoMensualEntity toEntity(SaldoMensual saldo) {
        SaldoMensualEntity entity = new SaldoMensualEntity();
        entity.id(saldo.getId());
        entity.afiliadoId(saldo.getAfiliadoId());
        entity.mes(saldo.getMes());
        entity.total(saldo.getTotal());
        entity.version(saldo.getVersion());
        return entity;
    }

    public SaldoMensual toDomain(SaldoMensualEntity entity) {
        return new SaldoMensual(
                entity.getId(),
                entity.getAfiliadoId(),
                entity.getMes(),
                entity.getTotal(),
                entity.getVersion()
        );
    }
}