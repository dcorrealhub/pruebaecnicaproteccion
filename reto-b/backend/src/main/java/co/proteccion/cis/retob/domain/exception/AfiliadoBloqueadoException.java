package co.proteccion.cis.retob.domain.exception;

public class AfiliadoBloqueadoException extends RuntimeException {

    public AfiliadoBloqueadoException(String afiliadoId) {
        super("El afiliado '" + afiliadoId + "' está bloqueado y no puede registrar aportes.");
    }
}
