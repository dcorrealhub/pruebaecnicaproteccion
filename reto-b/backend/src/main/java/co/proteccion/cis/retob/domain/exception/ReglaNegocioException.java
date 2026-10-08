package co.proteccion.cis.retob.domain.exception;

/**
 * Excepción base para violaciones de reglas de negocio del dominio.
 * Se mapea a HTTP 422 (Unprocessable Entity) en la capa web.
 * El mensaje debe ser claro y accionable, sin filtrar detalles internos.
 */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
