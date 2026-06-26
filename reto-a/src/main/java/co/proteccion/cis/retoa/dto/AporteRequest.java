package co.proteccion.cis.retoa.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Schema(description = "Solicitud de registro de un aporte voluntario al fondo de pensiones")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AporteRequest {

    @Schema(description = "Identificador único del afiliado", example = "AF-001", maxLength = 30)
    @NotBlank(message = "El afiliadoId es requerido")
    @Size(max = 30, message = "El afiliadoId no puede superar 30 caracteres")
    private String afiliadoId;

    @Schema(description = "Monto del aporte en pesos colombianos. Máximo 10.000.000 COP por mes.",
            example = "500000.00", minimum = "0.01", maximum = "10000000.00")
    @NotNull(message = "El monto es requerido")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    @DecimalMax(value = "10000000.00", message = "El monto no puede superar el tope mensual de 10.000.000")
    @Digits(integer = 11, fraction = 2, message = "El monto debe tener como máximo 11 dígitos enteros y 2 decimales")
    private BigDecimal monto;

    @Schema(description = "Canal de origen del aporte", example = "APP_MOVIL",
            allowableValues = {"APP_MOVIL", "WEB", "PRESENCIAL", "ATM"})
    @NotBlank(message = "El canal es requerido")
    @Pattern(regexp = "^(APP_MOVIL|WEB|PRESENCIAL|ATM)$",
             message = "Canal no válido. Valores aceptados: APP_MOVIL, WEB, PRESENCIAL, ATM")
    private String canal;

    @Schema(description = "Llave de idempotencia UUID v4. Reintentar con la misma llave devuelve el aporte original sin duplicar.",
            example = "550e8400-e29b-41d4-a716-446655440000")
    @NotBlank(message = "El idempotencyKey es requerido")
    @Pattern(
        regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$",
        message = "idempotencyKey debe ser un UUID v4 válido"
    )
    private String idempotencyKey;
}
