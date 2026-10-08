package co.proteccion.cis.retob.domain.exception;

/**
 * La solicitud es válida en forma pero viola una regla de negocio
 * (monto no positivo, tope mensual excedido...).
 */
public class ReglaNegocioException extends DominioException {

    public ReglaNegocioException(String codigo, String mensaje) {
        super(codigo, mensaje);
    }
}
