package co.proteccion.cis.retob.domain.exception;

/**
 * No se pudo completar la operación por conflicto de concurrencia tras agotar los reintentos.
 * Se mapea a HTTP 409 (Conflict) en la capa web.
 */
public class ConflictoConcurrenciaException extends RuntimeException {

    public ConflictoConcurrenciaException(String mensaje) {
        super(mensaje);
    }

    public ConflictoConcurrenciaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
