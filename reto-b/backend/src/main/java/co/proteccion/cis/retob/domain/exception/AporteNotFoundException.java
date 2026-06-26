package co.proteccion.cis.retob.domain.exception;

public class AporteNotFoundException extends RuntimeException {

    public AporteNotFoundException(String id) {
        super("Aporte no encontrado: id=" + id);
    }
}
