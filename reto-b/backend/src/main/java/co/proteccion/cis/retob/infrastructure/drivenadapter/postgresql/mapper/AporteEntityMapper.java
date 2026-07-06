package co.proteccion.cis.retob.infrastructure.drivenadapter.postgresql.mapper;

import co.proteccion.cis.retob.domain.model.aporte.Aporte;
import co.proteccion.cis.retob.infrastructure.drivenadapter.postgresql.entity.AporteEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AporteEntityMapper {

    Aporte toDomain(AporteEntity entity);

    List<Aporte> toDomainList(List<AporteEntity> entities);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    AporteEntity toEntity(Aporte domain);
}
