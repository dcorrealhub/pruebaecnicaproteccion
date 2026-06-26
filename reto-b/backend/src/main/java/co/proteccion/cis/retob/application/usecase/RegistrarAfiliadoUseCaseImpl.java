package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.model.Afiliado;
import co.proteccion.cis.retob.domain.model.EstadoAfiliado;
import co.proteccion.cis.retob.domain.port.in.RegistrarAfiliadoUseCase;
import co.proteccion.cis.retob.domain.port.out.AfiliadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegistrarAfiliadoUseCaseImpl implements RegistrarAfiliadoUseCase {

    private final AfiliadoRepository afiliadoRepository;

    @Override
    @Transactional
    public Afiliado registrar(RegistrarAfiliadoCommand command) {
        afiliadoRepository.findByAfiliadoId(command.afiliadoId()).ifPresent(a -> {
            throw new IllegalArgumentException("Ya existe un afiliado con id: " + command.afiliadoId());
        });

        Afiliado nuevo = new Afiliado(null, command.afiliadoId(), command.nombre(), EstadoAfiliado.ACTIVO, null);
        return afiliadoRepository.guardar(nuevo);
    }
}
