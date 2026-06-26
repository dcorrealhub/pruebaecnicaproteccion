package co.proteccion.cis.retob.domain.port.in;

import co.proteccion.cis.retob.domain.model.Afiliado;
import co.proteccion.cis.retob.domain.model.EstadoAfiliado;

public interface CambiarEstadoAfiliadoUseCase {

    Afiliado cambiar(CambiarEstadoAfiliadoCommand command);

    record CambiarEstadoAfiliadoCommand(
            String afiliadoId,
            EstadoAfiliado nuevoEstado
    ) {}
}
