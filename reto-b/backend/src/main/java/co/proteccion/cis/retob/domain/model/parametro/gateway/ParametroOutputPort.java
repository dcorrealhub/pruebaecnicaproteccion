package co.proteccion.cis.retob.domain.model.parametro.gateway;

import co.proteccion.cis.retob.domain.model.parametro.ParametrosAporte;

public interface ParametroOutputPort {

    /** Resuelve los parámetros de un afiliado: override por afiliado o, en su defecto, el global. */
    ParametrosAporte forAfiliado(String afiliadoId);

    /** Parámetros globales por defecto. */
    ParametrosAporte obtenerGlobal();

    /** Crea o actualiza los parámetros globales y devuelve los persistidos. */
    ParametrosAporte actualizarGlobal(ParametrosAporte parametros);
}
