package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.exception.AfiliadoNotFoundException;
import co.proteccion.cis.retob.domain.model.Afiliado;
import co.proteccion.cis.retob.domain.model.EstadoAfiliado;
import co.proteccion.cis.retob.domain.port.in.CambiarEstadoAfiliadoUseCase;
import co.proteccion.cis.retob.domain.port.out.AfiliadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CambiarEstadoAfiliadoUseCaseImpl implements CambiarEstadoAfiliadoUseCase {

    private final AfiliadoRepository afiliadoRepository;

    @Override
    @Transactional
    public Afiliado cambiar(CambiarEstadoAfiliadoCommand command) {
        Afiliado afiliado = afiliadoRepository.findByAfiliadoId(command.afiliadoId())
                .orElseThrow(() -> new AfiliadoNotFoundException(command.afiliadoId()));

        if (afiliado.getEstado() == command.nuevoEstado()) {
            throw new IllegalArgumentException(
                    "El afiliado '" + command.afiliadoId() +
                    "' ya se encuentra en estado " + command.nuevoEstado() + ".");
        }

        Afiliado actualizado = new Afiliado(
                afiliado.getId(),
                afiliado.getAfiliadoId(),
                afiliado.getNombre(),
                command.nuevoEstado(),
                afiliado.getCreadoEn()
        );

        // El trigger trg_afiliado_historial_estado registra el cambio automáticamente en DB
        return afiliadoRepository.guardar(actualizado);
    }
}
