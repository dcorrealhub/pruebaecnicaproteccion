package co.proteccion.cis.retob.infrastructure.adapter.rest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record AporteRequest(
        @NotBlank(message = "idAfiliado es requerido")
        @Size(max = 50, message = "idAfiliado maximo 50 caracteres")
        String idAfiliado,

        @NotNull(message = "monto es requerido")
        @DecimalMin(value = "0.0001", message = "monto debe ser mayor a cero")
        BigDecimal monto,

        @NotBlank(message = "canal es requerido")
        @Size(max = 50, message = "canal maximo 50 caracteres")
        String canal,

        @NotBlank(message = "idempotenciaKey es requerida")
        @Size(max = 100, message = "idempotenciaKey maxima 100 caracteres")
        String idempotenciaKey
) {}