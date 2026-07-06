package co.proteccion.cis.retob.infrastructure.drivenadapter.postgresql.mapper;

import co.proteccion.cis.retob.domain.model.aporte.SaldoMensual;
import co.proteccion.cis.retob.infrastructure.drivenadapter.postgresql.entity.SaldoMensualEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SaldoEntityMapper {

    SaldoMensual toDomain(SaldoMensualEntity entity);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    SaldoMensualEntity toEntity(SaldoMensual domain);
}
