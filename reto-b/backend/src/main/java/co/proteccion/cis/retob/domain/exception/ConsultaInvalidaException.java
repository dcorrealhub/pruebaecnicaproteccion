package co.proteccion.cis.retob.domain.exception;

/**
 * Los parámetros de una consulta violan una regla de negocio (ej. rango de periodos).
 */
public class ConsultaInvalidaException extends RuntimeException {

    private final String codigo;

    public ConsultaInvalidaException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() { return codigo; }
}
