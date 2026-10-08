package co.proteccion.cis.retob.domain.exception;

/**
 * Otra transacción modificó el mismo saldo mensual (o registró la misma clave) en paralelo.
 * Es transitorio: el cliente puede reintentar con la MISMA clave de idempotencia sin riesgo de duplicar.
 */
public class ConflictoConcurrenciaException extends DominioException {

    public static final String CODIGO = "CONFLICTO_CONCURRENCIA";

    public ConflictoConcurrenciaException(Throwable causa) {
        super(CODIGO, "El aporte no pudo registrarse por una operación concurrente. Reintente con la misma clave de idempotencia");
        initCause(causa);
    }
}
