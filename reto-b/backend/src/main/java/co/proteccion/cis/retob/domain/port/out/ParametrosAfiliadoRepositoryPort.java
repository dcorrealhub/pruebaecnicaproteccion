package co.proteccion.cis.retob.domain.port.out;

import co.proteccion.cis.retob.domain.model.ParametrosAfiliado;

import java.util.Optional;

/**
 * Puerto de salida: parámetros de negocio configurados para cada afiliado
 * (tope mensual, umbral de revisión). Vacío si el afiliado no tiene ninguno propio.
 */
public interface ParametrosAfiliadoRepositoryPort {

    Optional<ParametrosAfiliado> findByAfiliadoId(String afiliadoId);
}
