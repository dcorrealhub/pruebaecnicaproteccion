package co.proteccion.cis.retob.infrastructure.entrypoint.mapper;

import co.proteccion.cis.retob.domain.model.parametro.ParametrosAporte;
import co.proteccion.cis.retob.infrastructure.entrypoint.dto.request.ParametrosRequest;
import co.proteccion.cis.retob.infrastructure.entrypoint.dto.response.ParametrosResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ParametroRestMapper {

    ParametrosAporte toDomain(ParametrosRequest request);

    ParametrosResponse toResponse(ParametrosAporte domain);
}
