package co.proteccion.cis.retob.domain.exception;

/**
 * Violación de una regla de negocio con datos válidos (p. ej. superar el tope mensual
 * o una transición de estado no permitida). Se traduce a HTTP 422 (Unprocessable Entity).
 */
public class DomainBusinessRuleException extends RuntimeException {
    public DomainBusinessRuleException(String message) {
        super(message);
    }
}
