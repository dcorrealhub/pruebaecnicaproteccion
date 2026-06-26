package co.proteccion.cis.retob.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record RegistrarAfiliadoRequest(

        @NotBlank(message = "El afiliadoId es obligatorio")
        String afiliadoId,

        @NotBlank(message = "El nombre es obligatorio")
        String nombre
) {}
