package co.proteccion.cis.retob.infrastructure.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RegistrarAporteRequest(

        @NotBlank(message = "El afiliadoId es obligatorio")
        String afiliadoId,

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
        BigDecimal monto,

        @NotNull(message = "La fecha del aporte es obligatoria")
        LocalDate fecha,

        @NotBlank(message = "El canal es obligatorio")
        @Pattern(regexp = "APP_MOVIL|WEB|SUCURSAL",
                 message = "Canal no válido. Valores permitidos: APP_MOVIL, WEB, SUCURSAL")
        String canal,

        @NotBlank(message = "La clave de idempotencia es obligatoria")
        String idempotenciaKey
) {}
