package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.exception.AporteNotFoundException;
import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.RevisionAporte;
import co.proteccion.cis.retob.domain.port.in.CambiarEstadoAporteUseCase;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.RevisionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class CambiarEstadoAporteUseCaseImpl implements CambiarEstadoAporteUseCase {

    private final AporteRepositoryPort aporteRepository;
    private final RevisionRepository revisionRepository;

    @Override
    @Transactional
    public Aporte cambiar(CambiarEstadoCommand command) {
        Aporte aporte = aporteRepository.findById(command.aporteId())
                .orElseThrow(() -> new AporteNotFoundException(command.aporteId()));

        // Valida la transición — lanza TransicionEstadoInvalidaException si no es legal
        aporte.getEstado().transicionar(command.nuevoEstado());

        Aporte actualizado = new Aporte(
                aporte.getId(),
                aporte.getAfiliadoId(),
                aporte.getMonto(),
                aporte.getFecha(),
                aporte.getCanal(),
                aporte.getPeriodo(),
                command.nuevoEstado(),
                aporte.getIdempotenciaKey()
        );
        Aporte guardado = aporteRepository.guardar(actualizado);

        revisionRepository.guardar(new RevisionAporte(
                null,
                command.aporteId(),
                command.revisor(),
                command.nuevoEstado(),
                command.comentario(),
                OffsetDateTime.now()
        ));

        return guardado;
    }
}
