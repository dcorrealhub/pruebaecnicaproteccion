package co.proteccion.cis.retob.domain.model.parametro;

import java.math.BigDecimal;

/**
 * Parámetros de negocio configurables aplicables al registro de un aporte.
 * Objeto de valor puro; la validación de invariantes vive en el UseCase.
 */
public record ParametrosAporte(
        BigDecimal topeMensual,
        BigDecimal umbralRevision
) {
}
