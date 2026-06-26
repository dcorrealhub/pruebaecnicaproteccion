package co.proteccion.cis.retob.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record AnularAporteRequest(

        @NotBlank(message = "El afiliadoId es obligatorio para validar la titularidad")
        String afiliadoId,

        String motivo
) {}
