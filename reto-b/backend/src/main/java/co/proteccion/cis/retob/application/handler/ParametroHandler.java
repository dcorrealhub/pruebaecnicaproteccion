package co.proteccion.cis.retob.application.handler;

import co.proteccion.cis.retob.domain.model.parametro.ParametrosAporte;

public interface ParametroHandler {
    ParametrosAporte obtenerGlobal();
    ParametrosAporte actualizarGlobal(ParametrosAporte parametros);
}
