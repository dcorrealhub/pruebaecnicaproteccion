package co.proteccion.cis.retob.domain.port.in;

import co.proteccion.cis.retob.domain.model.ParametrosFondo;

import java.math.BigDecimal;

public interface ActualizarParametrosUseCase {

    ParametrosFondo actualizar(ActualizarParametrosCommand command);

    record ActualizarParametrosCommand(
            BigDecimal montoMinimo,
            BigDecimal topeMensual,
            BigDecimal umbralRevision,
            String modificadoPor,
            String comentario
    ) {}
}
