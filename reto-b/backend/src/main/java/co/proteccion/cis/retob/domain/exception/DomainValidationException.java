package co.proteccion.cis.retob.domain.exception;

/**
 * Datos de entrada inválidos según reglas del dominio. Se traduce a HTTP 400.
 */
public class DomainValidationException extends RuntimeException {
    public DomainValidationException(String message) {
        super(message);
    }
}
