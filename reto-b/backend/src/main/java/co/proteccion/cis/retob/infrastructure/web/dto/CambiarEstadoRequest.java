package co.proteccion.cis.retob.infrastructure.web.dto;

import co.proteccion.cis.retob.domain.model.EstadoAporte;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoRequest(

        @NotNull(message = "El nuevo estado es obligatorio")
        EstadoAporte nuevoEstado,

        @NotBlank(message = "El revisor es obligatorio")
        String revisor,

        String comentario
) {}
