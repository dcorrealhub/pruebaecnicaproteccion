package co.proteccion.cis.retob.domain.port.in;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.EstadoAporte;

public interface CambiarEstadoAporteUseCase {

    Aporte cambiar(CambiarEstadoCommand command);

    record CambiarEstadoCommand(
            Long aporteId,
            EstadoAporte nuevoEstado,
            String revisor,
            String comentario
    ) {}
}
