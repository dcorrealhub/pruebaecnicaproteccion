package co.proteccion.cis.retoa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AporteRequest {

    @NotBlank(message = "El afiliadoId es obligatorio")
    private String afiliadoId;

    // Monto del aporte en pesos colombianos
    @Positive(message = "El monto debe ser un valor positivo")
    private BigDecimal monto;

    @NotBlank(message = "El canal es obligatorio")
    private String canal;
}
