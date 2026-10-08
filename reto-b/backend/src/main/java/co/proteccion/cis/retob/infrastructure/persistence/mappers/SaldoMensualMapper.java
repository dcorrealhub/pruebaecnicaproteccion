package co.proteccion.cis.retob.infrastructure.persistence.mappers;

import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.infrastructure.persistence.entity.SaldoMensualEntity;

/**
 * Mapeo entre el modelo de dominio {@link SaldoMensual} y la entidad JPA {@link SaldoMensualEntity}.
 * La {@code version} se traslada en ambos sentidos para preservar el control optimista.
 */
public final class SaldoMensualMapper {

    private SaldoMensualMapper() {}

    public static SaldoMensualEntity toEntity(SaldoMensual saldo) {
        return SaldoMensualEntity.builder()
                .id(saldo.getId())
                .afiliadoId(saldo.getAfiliadoId())
                .mes(saldo.getMes())
                .total(saldo.getTotal())
                .version(saldo.getVersion())
                .build();
    }

    public static SaldoMensual toDomain(SaldoMensualEntity entity) {
        return new SaldoMensual(
                entity.getId(),
                entity.getAfiliadoId(),
                entity.getMes(),
                entity.getTotal(),
                entity.getVersion()
        );
    }
}
