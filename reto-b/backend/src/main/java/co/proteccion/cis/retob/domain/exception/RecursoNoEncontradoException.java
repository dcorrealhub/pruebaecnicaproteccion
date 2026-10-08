package co.proteccion.cis.retob.domain.exception;

/**
 * El recurso solicitado no existe. Se mapea a HTTP 404 (Not Found) en la capa web.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
