package co.proteccion.cis.retoa.exception;

public class AfiliadoNotFoundException extends RuntimeException {

    public AfiliadoNotFoundException() {
        super("Afiliado no encontrado");
    }
}
