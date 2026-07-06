package co.proteccion.cis.retob.domain.usecase.input;

import co.proteccion.cis.retob.domain.model.aporte.Aporte;
import co.proteccion.cis.retob.domain.model.aporte.ConsolidadoAportes;
import co.proteccion.cis.retob.domain.model.aporte.ConsultaConsolidado;

public interface AporteInputPort {

    /** Registra un aporte de forma idempotente por su clave. */
    Aporte registrar(Aporte aporte);

    /** Consolidado (total y detalle) de un afiliado en un rango de periodos. */
    ConsolidadoAportes consultar(ConsultaConsolidado consulta);

    /** Aprueba un aporte pendiente. */
    Aporte aprobar(Long aporteId);

    /** Rechaza un aporte pendiente y libera su reserva. */
    Aporte rechazar(Long aporteId);
}
