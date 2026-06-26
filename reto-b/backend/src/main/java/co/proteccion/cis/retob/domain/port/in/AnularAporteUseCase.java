package co.proteccion.cis.retob.domain.port.in;

import co.proteccion.cis.retob.domain.model.Aporte;

public interface AnularAporteUseCase {

    Aporte anular(AnularAporteCommand command);

    record AnularAporteCommand(
            String aporteId,
            String afiliadoId,
            String motivo
    ) {}
}
