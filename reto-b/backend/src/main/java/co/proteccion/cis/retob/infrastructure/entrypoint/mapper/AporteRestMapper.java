package co.proteccion.cis.retob.infrastructure.entrypoint.mapper;

import co.proteccion.cis.retob.domain.model.aporte.Aporte;
import co.proteccion.cis.retob.domain.model.aporte.ConsolidadoAportes;
import co.proteccion.cis.retob.infrastructure.entrypoint.dto.request.RegistrarAporteRequest;
import co.proteccion.cis.retob.infrastructure.entrypoint.dto.response.AporteResponse;
import co.proteccion.cis.retob.infrastructure.entrypoint.dto.response.ConsolidadoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AporteRestMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "periodo", ignore = true)
    @Mapping(target = "estado", ignore = true)
    Aporte toDomain(RegistrarAporteRequest request);

    @Mapping(target = "marcadaRevision",
            expression = "java(domain.getEstado() == co.proteccion.cis.retob.domain.model.aporte.EstadoAporte.PENDIENTE_REVISION)")
    AporteResponse toResponse(Aporte domain);

    List<AporteResponse> toResponseList(List<Aporte> domains);

    ConsolidadoResponse toResponse(ConsolidadoAportes consolidado);
}
