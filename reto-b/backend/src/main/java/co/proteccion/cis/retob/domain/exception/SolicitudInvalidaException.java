package co.proteccion.cis.retob.domain.exception;

/**
 * Los datos de entrada no tienen la forma esperada (canal desconocido, periodo mal formado...).
 */
public class SolicitudInvalidaException extends DominioException {

    public SolicitudInvalidaException(String codigo, String mensaje) {
        super(codigo, mensaje);
    }
}
