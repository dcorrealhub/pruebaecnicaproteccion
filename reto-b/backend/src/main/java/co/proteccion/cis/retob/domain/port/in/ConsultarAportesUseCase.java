package co.proteccion.cis.retob.domain.port.in;

import co.proteccion.cis.retob.domain.model.ConsolidadoAportes;

/**
 * Puerto de entrada (caso de uso): consultar el consolidado de aportes.
 * Solo lectura — no modifica estado.
 */
public interface ConsultarAportesUseCase {

    /**
     * Retorna el consolidado de aportes de un afiliado en el periodo indicado.
     *
     * @param query parámetros de consulta
     * @return consolidado con total y detalle
     */
    ConsolidadoAportes consultar(ConsultarAportesQuery query);

    record ConsultarAportesQuery(
            String afiliadoId,
            String periodoDesde,  // formato YYYY-MM
            String periodoHasta   // formato YYYY-MM
    ) {}
}
