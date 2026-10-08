package co.proteccion.cis.retob.infrastructure.web.dto;

import co.proteccion.cis.retob.domain.model.Canal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
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
        @Digits(integer = 17, fraction = 2, message = "El monto admite hasta 2 decimales")
        BigDecimal monto,

        @NotNull(message = "La fecha es obligatoria")
        @PastOrPresent(message = "La fecha del aporte no puede ser futura")
        LocalDate fecha,

        @NotNull(message = "El canal es obligatorio")
        Canal canal,

        @NotBlank(message = "La clave de idempotencia es obligatoria")
        String idempotenciaKey
) {}
