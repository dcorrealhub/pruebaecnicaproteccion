package co.proteccion.cis.retob.domain.exception;

public abstract class DomainException extends RuntimeException {

    private final String codigo;

    protected DomainException(String codigo, String message) {
        super(message);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
