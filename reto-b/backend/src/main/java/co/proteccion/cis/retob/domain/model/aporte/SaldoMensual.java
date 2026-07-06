package co.proteccion.cis.retob.domain.model.aporte;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Acumulado mensual comprometido (aprobados + pendientes) por afiliado.
 * El campo {@code version} soporta bloqueo optimista en la persistencia.
 */
@Data
@Builder(toBuilder = true)
public class SaldoMensual {
    private Long id;
    private String afiliadoId;
    private String mes;
    private BigDecimal total;
    private Integer version;
}
