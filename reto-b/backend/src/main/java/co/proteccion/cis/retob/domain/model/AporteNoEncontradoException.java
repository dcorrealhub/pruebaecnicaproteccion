package co.proteccion.cis.retob.domain.model;

/**
 * Se lanza al referenciar un aporte inexistente (p. ej. al aprobar/rechazar por id).
 * La capa web la traduce a HTTP 404.
 */
public class AporteNoEncontradoException extends RuntimeException {

    public AporteNoEncontradoException(Long aporteId) {
        super("No existe un aporte con id " + aporteId);
    }
}
