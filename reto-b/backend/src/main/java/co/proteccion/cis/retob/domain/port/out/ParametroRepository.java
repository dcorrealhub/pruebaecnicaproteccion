package co.proteccion.cis.retob.domain.port.out;

import co.proteccion.cis.retob.domain.model.ParametrosFondo;

import java.util.List;
import java.util.Optional;

public interface ParametroRepository {

    ParametrosFondo guardarCambio(ParametrosFondo parametros);

    Optional<ParametrosFondo> findLatest();

    List<ParametrosFondo> findAll();
}
