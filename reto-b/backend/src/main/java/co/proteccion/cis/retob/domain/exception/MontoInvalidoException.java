package co.proteccion.cis.retob.domain.exception;

/**
 * El monto del aporte no es válido (no positivo o nulo).
 */
public class MontoInvalidoException extends ReglaNegocioException {

    public MontoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
