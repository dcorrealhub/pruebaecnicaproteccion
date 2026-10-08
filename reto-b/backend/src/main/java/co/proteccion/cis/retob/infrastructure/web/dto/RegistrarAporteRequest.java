package co.proteccion.cis.retob.infrastructure.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * La fecha no viaja en el request: la asigna el servidor.
 * Los límites de longitud reflejan las columnas de la tabla, para rechazar con 400
 * en lugar de fallar con un error de base de datos.
 */
public record RegistrarAporteRequest(

        @NotBlank(message = "El afiliadoId es obligatorio")
        @Pattern(regexp = "^[A-Za-z0-9-]{1,50}$",
                 message = "El afiliadoId solo admite letras, números y guiones (máx. 50)")
        String afiliadoId,

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
        @Digits(integer = 13, fraction = 2, message = "El monto admite máximo 13 enteros y 2 decimales")
        BigDecimal monto,

        @NotBlank(message = "El canal es obligatorio")
        String canal,

        @NotBlank(message = "La clave de idempotencia es obligatoria")
        @Pattern(regexp = "^[A-Za-z0-9-]{8,100}$",
                 message = "La clave de idempotencia debe tener entre 8 y 100 caracteres alfanuméricos o guiones")
        String idempotenciaKey
) {}
