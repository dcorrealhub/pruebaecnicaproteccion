package co.proteccion.cis.retob.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ConsultarConsolidadoRequest(

        @NotBlank(message = "El afiliadoId es obligatorio")
        String afiliadoId,

        @NotBlank(message = "El periodoDesde es obligatorio")
        @Pattern(regexp = "\\d{4}-\\d{2}", message = "periodoDesde debe tener formato YYYY-MM")
        String periodoDesde,

        @NotBlank(message = "El periodoHasta es obligatorio")
        @Pattern(regexp = "\\d{4}-\\d{2}", message = "periodoHasta debe tener formato YYYY-MM")
        String periodoHasta
) {}
