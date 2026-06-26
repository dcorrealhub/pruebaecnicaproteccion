package co.proteccion.cis.retob.domain.model;

import java.math.BigDecimal;

/**
 * Parámetros de negocio configurables que aplican al registro de un aporte.
 * Resueltos por afiliado (con posible override) desde la capa de persistencia.
 *
 * @param topeMensual    monto máximo acumulado permitido por afiliado y mes
 * @param umbralRevision a partir de este monto un aporte queda marcado para revisión
 */
public record ParametrosAporte(BigDecimal topeMensual, BigDecimal umbralRevision) {

    public ParametrosAporte {
        if (topeMensual == null || topeMensual.signum() <= 0) {
            throw new IllegalArgumentException("El tope mensual debe ser positivo");
        }
        if (umbralRevision == null || umbralRevision.signum() <= 0) {
            throw new IllegalArgumentException("El umbral de revisión debe ser positivo");
        }
        if (umbralRevision.compareTo(topeMensual) > 0) {
            throw new IllegalArgumentException("El umbral de revisión no puede ser mayor que el tope mensual");
        }
    }

    public boolean superaUmbral(BigDecimal monto) {
        return monto.compareTo(umbralRevision) > 0;
    }

    public boolean superaTope(BigDecimal totalAcumulado) {
        return totalAcumulado.compareTo(topeMensual) > 0;
    }
}
