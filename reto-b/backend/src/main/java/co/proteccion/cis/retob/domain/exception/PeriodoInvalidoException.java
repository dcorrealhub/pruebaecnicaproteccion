package co.proteccion.cis.retob.domain.exception;

public class PeriodoInvalidoException extends DomainException {

    public PeriodoInvalidoException(String mensaje) {
        super("PERIODO_INVALIDO", mensaje);
    }
}
