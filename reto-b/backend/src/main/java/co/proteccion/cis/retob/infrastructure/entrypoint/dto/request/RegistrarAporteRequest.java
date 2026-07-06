package co.proteccion.cis.retob.infrastructure.entrypoint.dto.request;

import co.proteccion.cis.retob.domain.constant.AporteMessages;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Los mensajes de campos que también son invariantes de dominio se toman de
 * {@link AporteMessages}, de modo que la validación del request y la del UseCase
 * hablen con una sola voz.
 */
public record RegistrarAporteRequest(

        @NotBlank(message = AporteMessages.AFILIADO_REQUERIDO)
        String afiliadoId,

        @NotNull(message = AporteMessages.MONTO_POSITIVO)
        @DecimalMin(value = "0.01", message = AporteMessages.MONTO_POSITIVO)
        BigDecimal monto,

        // Concern exclusivo del canal HTTP (el dominio asume "hoy" si viene null): mensaje literal.
        @PastOrPresent(message = "La fecha no puede ser futura")
        LocalDate fecha,

        @NotBlank(message = AporteMessages.CANAL_REQUERIDO)
        String canal,

        @NotBlank(message = AporteMessages.IDEMPOTENCIA_REQUERIDA)
        String idempotenciaKey
) {
}
