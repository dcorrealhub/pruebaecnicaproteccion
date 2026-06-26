package co.proteccion.cis.retob.domain.model;

/**
 * Se lanza cuando una operación viola una regla de negocio (p. ej. el aporte
 * supera el tope mensual, o se intenta una transición de estado inválida).
 *
 * <p>La capa web la traduce a una respuesta HTTP 422 con un mensaje claro para
 * el cliente, de modo que los aportes rechazados se comuniquen sin ambigüedad.
 */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
