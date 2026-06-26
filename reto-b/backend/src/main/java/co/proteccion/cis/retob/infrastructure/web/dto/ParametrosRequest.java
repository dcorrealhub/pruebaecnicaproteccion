package co.proteccion.cis.retob.infrastructure.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ParametrosRequest(

        @NotNull(message = "El tope mensual es obligatorio")
        @DecimalMin(value = "0.01", message = "El tope mensual debe ser mayor a cero")
        BigDecimal topeMensual,

        @NotNull(message = "El umbral de revisión es obligatorio")
        @DecimalMin(value = "0.01", message = "El umbral de revisión debe ser mayor a cero")
        BigDecimal umbralRevision
) {}
