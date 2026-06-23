package co.proteccion.cis.retob.infrastructure.persistence.mapper;

import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.infrastructure.persistence.entity.SaldoMensualEntity;
import org.springframework.stereotype.Component;

@Component
public class SaldoPersistenceMapper {

    public SaldoMensual toDomain(SaldoMensualEntity entity) {
        return new SaldoMensual(
                entity.getId(),
                entity.getAfiliadoId(),
                entity.getMes(),
                entity.getTotal(),
                entity.getVersion()
        );
    }

    public SaldoMensualEntity toEntity(SaldoMensual saldo) {
        return SaldoMensualEntity.builder()
                .id(saldo.getId())
                .afiliadoId(saldo.getAfiliadoId())
                .mes(saldo.getMes())
                .total(saldo.getTotal())
                .version(saldo.getVersion())
                .build();
    }
}
