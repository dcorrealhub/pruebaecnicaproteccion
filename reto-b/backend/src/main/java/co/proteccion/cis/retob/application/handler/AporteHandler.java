package co.proteccion.cis.retob.application.handler;

import co.proteccion.cis.retob.domain.model.aporte.Aporte;
import co.proteccion.cis.retob.domain.model.aporte.ConsolidadoAportes;
import co.proteccion.cis.retob.domain.model.aporte.ConsultaConsolidado;

public interface AporteHandler {
    Aporte registrar(Aporte aporte);
    ConsolidadoAportes consultar(ConsultaConsolidado consulta);
    Aporte aprobar(Long aporteId);
    Aporte rechazar(Long aporteId);
}
