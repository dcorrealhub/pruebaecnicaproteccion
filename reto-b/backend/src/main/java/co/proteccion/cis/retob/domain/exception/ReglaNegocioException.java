package co.proteccion.cis.retob.domain.exception;

/**
 * Excepción lanzada cuando se viola una regla de negocio del dominio.
 * El código identifica la regla para facilitar el manejo diferenciado en capas superiores.
 */
public class ReglaNegocioException extends RuntimeException {

    private final String codigo;

    public ReglaNegocioException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
