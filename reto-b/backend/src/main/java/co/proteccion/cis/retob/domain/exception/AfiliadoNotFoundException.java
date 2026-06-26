package co.proteccion.cis.retob.domain.exception;

public class AfiliadoNotFoundException extends RuntimeException {

    public AfiliadoNotFoundException(String afiliadoId) {
        super("Afiliado no encontrado: afiliadoId=" + afiliadoId);
    }
}
