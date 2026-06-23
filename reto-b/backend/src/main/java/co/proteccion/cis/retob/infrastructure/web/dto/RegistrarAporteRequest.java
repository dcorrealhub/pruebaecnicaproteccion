package co.proteccion.cis.retob.infrastructure.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RegistrarAporteRequest(

        @NotBlank(message = "El afiliadoId es obligatorio")
        String afiliadoId,

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
        BigDecimal monto,

        @NotBlank(message = "El canal es obligatorio")
        String canal,

        String idempotenciaKey
) {}
