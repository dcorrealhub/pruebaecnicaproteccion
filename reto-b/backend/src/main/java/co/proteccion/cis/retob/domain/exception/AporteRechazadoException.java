package co.proteccion.cis.retob.domain.exception;

/**
 * Un aporte viola una regla de negocio (monto, fecha, tope mensual) y se rechaza.
 */
public class AporteRechazadoException extends RuntimeException {

    private final String codigo;

    public AporteRechazadoException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() { return codigo; }
}
