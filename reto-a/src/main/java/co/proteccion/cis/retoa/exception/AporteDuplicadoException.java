package co.proteccion.cis.retoa.exception;

public class AporteDuplicadoException extends RuntimeException {

    public AporteDuplicadoException() {
        super("Aporte duplicado: llave de idempotencia ya utilizada");
    }
}
