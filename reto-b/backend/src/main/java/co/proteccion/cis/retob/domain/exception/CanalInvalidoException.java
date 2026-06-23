package co.proteccion.cis.retob.domain.exception;

public class CanalInvalidoException extends DomainException {

    public CanalInvalidoException(String canal) {
        super("CANAL_INVALIDO", "Canal de aporte no reconocido: " + canal);
    }
}
