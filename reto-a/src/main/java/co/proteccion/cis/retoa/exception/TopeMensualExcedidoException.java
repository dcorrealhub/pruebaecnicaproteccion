package co.proteccion.cis.retoa.exception;

public class TopeMensualExcedidoException extends RuntimeException {

    public TopeMensualExcedidoException() {
        super("El monto supera el tope mensual permitido");
    }
}
