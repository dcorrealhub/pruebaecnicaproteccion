package co.proteccion.cis.retob.domain.model;

import java.math.BigDecimal;

/**
 * Parámetros de negocio propios de un afiliado. Cada valor es opcional ({@code null}):
 * cuando falta, aplica el valor por defecto de la configuración.
 */
public record ParametrosAfiliado(BigDecimal topeMensual, BigDecimal umbralRevision) {

    /** Afiliado sin parámetros propios: todo se resuelve con los valores por defecto. */
    public static final ParametrosAfiliado POR_DEFECTO = new ParametrosAfiliado(null, null);
}
