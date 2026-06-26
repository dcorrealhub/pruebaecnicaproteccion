package co.proteccion.cis.retob.infrastructure.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RegistrarAporteRequest(

        @NotBlank(message = "El afiliadoId es obligatorio")
        String afiliadoId,

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
        BigDecimal monto,

        // Opcional: si no se envía, el servidor asume la fecha actual.
        @PastOrPresent(message = "La fecha no puede ser futura")
        LocalDate fecha,

        @NotBlank(message = "El canal es obligatorio")
        String canal,

        @NotBlank(message = "La clave de idempotencia es obligatoria")
        String idempotenciaKey
) {}
