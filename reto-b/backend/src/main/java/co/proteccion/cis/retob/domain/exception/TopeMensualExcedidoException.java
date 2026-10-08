package co.proteccion.cis.retob.domain.exception;

/**
 * El aporte haría que el acumulado del mes supere el tope mensual del afiliado.
 */
public class TopeMensualExcedidoException extends ReglaNegocioException {

    public TopeMensualExcedidoException(String mensaje) {
        super(mensaje);
    }
}
