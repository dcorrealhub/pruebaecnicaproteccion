package co.proteccion.cis.retob.infrastructure.web.dto;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Cuerpo de error uniforme para respuestas no exitosas.
 *
 * @param errores detalle por campo en errores de validación (puede ser null)
 */
public record ErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String mensaje,
        Map<String, String> errores
) {
    public static ErrorResponse of(int status, String error, String mensaje) {
        return new ErrorResponse(OffsetDateTime.now(), status, error, mensaje, null);
    }

    public static ErrorResponse ofValidacion(int status, String error, String mensaje, Map<String, String> errores) {
        return new ErrorResponse(OffsetDateTime.now(), status, error, mensaje, errores);
    }
}
