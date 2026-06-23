package co.proteccion.cis.retob.domain.exception;

import java.math.BigDecimal;

public class MontoInvalidoException extends DomainException {

    public MontoInvalidoException(BigDecimal monto) {
        super("MONTO_INVALIDO", "El monto debe ser mayor a cero: " + monto);
    }
}
