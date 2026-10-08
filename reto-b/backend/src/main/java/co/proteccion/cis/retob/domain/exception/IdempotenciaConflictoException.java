package co.proteccion.cis.retob.domain.exception;

/**
 * Se reutilizó una clave de idempotencia con un contenido distinto al del aporte original.
 */
public class IdempotenciaConflictoException extends RuntimeException {

    public static final String CODIGO = "IDEMPOTENCIA_CONFLICTO";

    public IdempotenciaConflictoException(String idempotenciaKey) {
        super("La clave de idempotencia '" + idempotenciaKey
                + "' ya fue usada para un aporte con datos distintos");
    }

    public String getCodigo() { return CODIGO; }
}
