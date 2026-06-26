package co.proteccion.cis.retob.domain.exception;

import java.math.BigDecimal;

public class MontoMinimoNoAlcanzadoException extends RuntimeException {

    public MontoMinimoNoAlcanzadoException(BigDecimal montoMinimo, BigDecimal montoRecibido) {
        super(String.format(
                "El monto del aporte (%s) es menor al mínimo permitido (%s).",
                montoRecibido, montoMinimo));
    }
}
