package co.proteccion.cis.retob.domain.exception;

/**
 * Recurso de dominio inexistente. La capa REST la traduce a HTTP 404.
 */
public class DomainNotFoundException extends RuntimeException {
    public DomainNotFoundException(String message) {
        super(message);
    }
}
