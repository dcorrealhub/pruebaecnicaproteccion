package co.proteccion.cis.retoa.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AporteRequest {

    @NotBlank(message = "afiliadoId es obligatorio")
    private String afiliadoId;

    @NotNull(message = "monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    private BigDecimal monto;

    @NotBlank(message = "canal es obligatorio")
    private String canal;
}