package co.proteccion.cis.retoa.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AporteRequest {

    @NotBlank(message = "El afiliadoId es requerido")
    @Size(max = 30, message = "El afiliadoId no puede superar 30 caracteres")
    private String afiliadoId;

    @NotNull(message = "El monto es requerido")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    @DecimalMax(value = "10000000.00", message = "El monto no puede superar el tope mensual de 10.000.000")
    @Digits(integer = 11, fraction = 2, message = "El monto debe tener como máximo 11 dígitos enteros y 2 decimales")
    private BigDecimal monto;

    @NotBlank(message = "El canal es requerido")
    @Pattern(regexp = "^(APP_MOVIL|WEB|PRESENCIAL|ATM)$",
             message = "Canal no válido. Valores aceptados: APP_MOVIL, WEB, PRESENCIAL, ATM")
    private String canal;
}
