package co.proteccion.cis.retob.domain.port.in;

import co.proteccion.cis.retob.domain.model.Afiliado;

public interface RegistrarAfiliadoUseCase {

    Afiliado registrar(RegistrarAfiliadoCommand command);

    record RegistrarAfiliadoCommand(
            String afiliadoId,
            String nombre
    ) {}
}
