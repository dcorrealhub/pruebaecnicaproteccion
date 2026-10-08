package co.proteccion.cis.retob.domain.exception;

/**
 * Se reutilizó una clave de idempotencia con un contenido distinto al del aporte original.
 * Es un error del cliente: reintentar no lo resuelve.
 */
public class ConflictoIdempotenciaException extends DominioException {

    public static final String CODIGO = "IDEMPOTENCIA_CONFLICTO";

    public ConflictoIdempotenciaException() {
        super(CODIGO, "La clave de idempotencia ya fue usada para un aporte con datos distintos");
    }
}
