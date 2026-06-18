package co.proteccion.cis.retoa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AporteRequest {

    private String afiliadoId;

    // Monto del aporte en pesos colombianos
    private double monto;

    private String canal;
}
