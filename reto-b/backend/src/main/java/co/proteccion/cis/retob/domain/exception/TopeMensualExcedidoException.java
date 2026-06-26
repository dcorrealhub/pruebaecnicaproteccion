package co.proteccion.cis.retob.domain.exception;

import java.math.BigDecimal;

public class TopeMensualExcedidoException extends RuntimeException {

    public TopeMensualExcedidoException(BigDecimal topeMensual, BigDecimal totalActual, BigDecimal montoNuevo) {
        super(String.format(
                "Tope mensual excedido: tope=%s, acumulado=%s, monto solicitado=%s",
                topeMensual, totalActual, montoNuevo));
    }
}
