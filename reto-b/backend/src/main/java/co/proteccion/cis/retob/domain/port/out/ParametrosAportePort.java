package co.proteccion.cis.retob.domain.port.out;

import co.proteccion.cis.retob.domain.model.ParametrosAporte;

/**
 * Puerto de salida: resuelve los parámetros de negocio (tope mensual y umbral de revisión)
 * aplicables a un afiliado en un mes dado.
 *
 * <p>La firma recibe {@code afiliadoId} y {@code mes} deliberadamente: hoy la implementación
 * devuelve valores globales (configuración), pero extender a parámetros por afiliado y/o mes
 * es solo cambiar la implementación de este puerto, sin tocar el caso de uso ni el dominio.</p>
 */
public interface ParametrosAportePort {

    /**
     * @param afiliadoId id sintético del afiliado
     * @param mes        periodo en formato YYYY-MM
     * @return parámetros aplicables (nunca null)
     */
    ParametrosAporte resolver(String afiliadoId, String mes);
}
