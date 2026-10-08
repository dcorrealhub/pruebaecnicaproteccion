package co.proteccion.cis.retob.domain.exception;

/**
 * Base de las excepciones de dominio. Cada una lleva un código estable que el
 * cliente puede usar para decidir qué hacer, independiente del texto del mensaje.
 */
public abstract class DominioException extends RuntimeException {

    private final String codigo;

    protected DominioException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
