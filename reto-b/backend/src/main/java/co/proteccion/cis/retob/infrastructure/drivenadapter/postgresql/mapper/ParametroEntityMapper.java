package co.proteccion.cis.retob.infrastructure.drivenadapter.postgresql.mapper;

import co.proteccion.cis.retob.domain.model.parametro.ParametrosAporte;
import co.proteccion.cis.retob.infrastructure.drivenadapter.postgresql.entity.ParametroAporteEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ParametroEntityMapper {

    ParametrosAporte toDomain(ParametroAporteEntity entity);
}
