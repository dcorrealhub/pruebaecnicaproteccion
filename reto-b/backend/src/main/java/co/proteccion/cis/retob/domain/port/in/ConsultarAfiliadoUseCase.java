package co.proteccion.cis.retob.domain.port.in;

import co.proteccion.cis.retob.domain.model.Afiliado;

import java.util.List;

public interface ConsultarAfiliadoUseCase {

    Afiliado consultar(String afiliadoId);

    List<Afiliado> consultarTodos();
}
