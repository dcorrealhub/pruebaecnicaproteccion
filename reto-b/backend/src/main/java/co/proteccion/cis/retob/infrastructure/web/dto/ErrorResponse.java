package co.proteccion.cis.retob.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int status,
        String error,
        String mensaje,
        String path,
        OffsetDateTime timestamp,
        Map<String, String> campos   // solo para errores de validación
) {
    public static ErrorResponse of(int status, String error, String mensaje, String path) {
        return new ErrorResponse(status, error, mensaje, path, OffsetDateTime.now(), null);
    }

    public static ErrorResponse of(int status, String error, String mensaje, String path,
                                   Map<String, String> campos) {
        return new ErrorResponse(status, error, mensaje, path, OffsetDateTime.now(), campos);
    }
}
