package co.proteccion.cis.retob.infrastructure.web.dto;

import java.time.OffsetDateTime;

public record ErrorResponse(
        String codigo,
        String mensaje,
        OffsetDateTime timestamp
) {
    public static ErrorResponse of(String codigo, String mensaje) {
        return new ErrorResponse(codigo, mensaje, OffsetDateTime.now());
    }
}
