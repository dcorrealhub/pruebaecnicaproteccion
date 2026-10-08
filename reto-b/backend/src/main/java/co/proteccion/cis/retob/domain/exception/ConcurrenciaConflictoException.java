package co.proteccion.cis.retob.domain.exception;

/**
 * Otra operación concurrente modificó los mismos datos. La operación puede reintentarse
 * de forma segura con la misma clave de idempotencia.
 */
public class ConcurrenciaConflictoException extends RuntimeException {

    public static final String CODIGO = "CONFLICTO_CONCURRENCIA";

    public ConcurrenciaConflictoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    public String getCodigo() { return CODIGO; }
}
