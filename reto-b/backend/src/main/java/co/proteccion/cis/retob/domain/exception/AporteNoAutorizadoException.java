package co.proteccion.cis.retob.domain.exception;

public class AporteNoAutorizadoException extends RuntimeException {

    public AporteNoAutorizadoException(String aporteId, String afiliadoId) {
        super("El aporte '" + aporteId + "' no pertenece al afiliado '" + afiliadoId + "'.");
    }
}
