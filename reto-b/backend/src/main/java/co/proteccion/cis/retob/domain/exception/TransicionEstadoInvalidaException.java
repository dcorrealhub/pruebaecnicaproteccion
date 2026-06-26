package co.proteccion.cis.retob.domain.exception;

public class TransicionEstadoInvalidaException extends RuntimeException {

    public TransicionEstadoInvalidaException(Enum<?> actual, Enum<?> solicitado) {
        super(String.format("Transición de estado inválida: %s → %s", actual, solicitado));
    }
}
