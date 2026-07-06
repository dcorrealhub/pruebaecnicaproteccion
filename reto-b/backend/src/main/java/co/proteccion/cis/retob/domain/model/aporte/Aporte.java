package co.proteccion.cis.retob.domain.model.aporte;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Modelo de dominio de un aporte voluntario. Datos puros (Lombok); la lógica de negocio
 * vive en el UseCase. El {@code periodo} (YYYY-MM) lo deriva el UseCase a partir de la fecha.
 */
@Data
@Builder(toBuilder = true)
public class Aporte {
    private Long id;
    private String afiliadoId;
    private BigDecimal monto;
    private LocalDate fecha;
    private String canal;
    private String periodo;
    private EstadoAporte estado;
    private String idempotenciaKey;
}
