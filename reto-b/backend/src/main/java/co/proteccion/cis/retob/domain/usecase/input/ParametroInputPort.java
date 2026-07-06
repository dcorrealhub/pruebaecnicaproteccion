package co.proteccion.cis.retob.domain.usecase.input;

import co.proteccion.cis.retob.domain.model.parametro.ParametrosAporte;

public interface ParametroInputPort {

    ParametrosAporte obtenerGlobal();

    ParametrosAporte actualizarGlobal(ParametrosAporte parametros);
}
