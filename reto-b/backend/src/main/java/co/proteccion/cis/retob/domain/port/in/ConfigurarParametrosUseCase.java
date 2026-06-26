package co.proteccion.cis.retob.domain.port.in;

import co.proteccion.cis.retob.domain.model.ParametrosAporte;

import java.math.BigDecimal;

/**
 * Puerto de entrada: consulta y actualización de los parámetros globales configurables
 * (tope mensual y umbral de revisión) que aplican al registro de aportes.
 */
public interface ConfigurarParametrosUseCase {

    ParametrosAporte obtenerGlobal();

    /**
     * Actualiza los parámetros globales. La validación (positivos, umbral ≤ tope) vive
     * en el dominio ({@link ParametrosAporte}).
     *
     * @throws IllegalArgumentException si los valores son inválidos
     */
    ParametrosAporte actualizarGlobal(BigDecimal topeMensual, BigDecimal umbralRevision);
}
