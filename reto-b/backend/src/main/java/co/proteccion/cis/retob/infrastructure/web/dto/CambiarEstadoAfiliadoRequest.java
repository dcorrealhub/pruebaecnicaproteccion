package co.proteccion.cis.retob.infrastructure.web.dto;

import co.proteccion.cis.retob.domain.model.EstadoAfiliado;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoAfiliadoRequest(

        @NotNull(message = "El nuevo estado es obligatorio")
        EstadoAfiliado nuevoEstado
) {}
