package co.proteccion.cis.retob.domain.port.in;

import co.proteccion.cis.retob.domain.model.ParametrosFondo;

import java.util.List;
import java.util.Optional;

public interface ConsultarParametrosUseCase {

    Optional<ParametrosFondo> consultarActual();

    List<ParametrosFondo> consultarHistorial();
}
