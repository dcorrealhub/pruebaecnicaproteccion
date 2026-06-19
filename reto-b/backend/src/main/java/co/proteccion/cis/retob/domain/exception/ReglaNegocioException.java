package co.proteccion.cis.retob.domain.exception;

/**
 * Excepción de dominio para violaciones de reglas de negocio.
 * No extiende checked exceptions: el dominio no debe conocer el transporte.
 */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String message) {
        super(message);
    }
}
