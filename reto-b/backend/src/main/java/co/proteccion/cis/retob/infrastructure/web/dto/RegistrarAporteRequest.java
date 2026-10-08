package co.proteccion.cis.retob.infrastructure.web.dto;

import co.proteccion.cis.retob.domain.model.enums.Canal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RegistrarAporteRequest(

        @NotBlank(message = "El afiliadoId es obligatorio")
        @Size(max = 50, message = "El afiliadoId admite máximo 50 caracteres")
        String afiliadoId,

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
        @Digits(integer = 13, fraction = 2, message = "El monto admite máximo 13 enteros y 2 decimales")
        BigDecimal monto,

        @NotNull(message = "La fecha es obligatoria")
        LocalDate fecha,

        @NotNull(message = "El canal es obligatorio")
        Canal canal,

        @NotBlank(message = "La clave de idempotencia es obligatoria")
        @Size(max = 100, message = "La clave de idempotencia admite máximo 100 caracteres")
        String idempotenciaKey
) {}
