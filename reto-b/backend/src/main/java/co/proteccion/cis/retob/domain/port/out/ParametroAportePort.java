package co.proteccion.cis.retob.domain.port.out;

import co.proteccion.cis.retob.domain.model.ParametrosAporte;

/**
 * Puerto de salida: resuelve los parámetros de negocio configurables
 * (tope mensual y umbral de revisión) aplicables a un afiliado.
 *
 * <p>La implementación los lee desde la BD, prefiriendo un override por afiliado
 * sobre el valor global por defecto.
 */
public interface ParametroAportePort {

    ParametrosAporte forAfiliado(String afiliadoId);

    /** Parámetros globales por defecto (fila con afiliado_id NULL). */
    ParametrosAporte obtenerGlobal();

    /** Crea o actualiza los parámetros globales por defecto y devuelve los valores persistidos. */
    ParametrosAporte actualizarGlobal(ParametrosAporte params);
}
