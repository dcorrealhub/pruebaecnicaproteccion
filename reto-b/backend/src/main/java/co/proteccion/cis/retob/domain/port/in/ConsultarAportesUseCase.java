package co.proteccion.cis.retob.domain.port.in;

import co.proteccion.cis.retob.domain.model.ConsolidadoAportes;

public interface ConsultarAportesUseCase {

    ConsolidadoAportes consultar(String afiliadoId, String periodoDesde, String periodoHasta);
}
