package co.proteccion.cis.retob.domain.port.in;

import co.proteccion.cis.retob.domain.model.Aporte;
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

    /**
     * Busca un aporte por su identificador.
     *
     * @param id identificador del aporte
     * @return el aporte
     * @throws co.proteccion.cis.retob.domain.exception.RecursoNoEncontradoException si no existe
     */
    Aporte buscarPorId(Long id);

    record ConsultarAportesQuery(
            String afiliadoId,
            String periodoDesde,  // formato YYYY-MM
            String periodoHasta   // formato YYYY-MM
    ) {}
}
