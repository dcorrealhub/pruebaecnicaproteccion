package co.proteccion.cis.retob.infrastructure.adapter.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record ConsolidadoRequest(
        @NotBlank(message = "idAfiliado es requerido")
        String idAfiliado,

        @NotBlank(message = "periodoDesde es requerido")
        String periodoDesde,

        @NotBlank(message = "periodoHasta es requerido")
        String periodoHasta
) {}