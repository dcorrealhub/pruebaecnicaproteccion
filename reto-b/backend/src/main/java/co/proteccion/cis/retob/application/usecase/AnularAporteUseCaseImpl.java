package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.exception.AporteNoAutorizadoException;
import co.proteccion.cis.retob.domain.exception.AporteNotFoundException;
import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.EstadoAporte;
import co.proteccion.cis.retob.domain.model.RevisionAporte;
import co.proteccion.cis.retob.domain.port.in.AnularAporteUseCase;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.RevisionRepository;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class AnularAporteUseCaseImpl implements AnularAporteUseCase {

    private final AporteRepositoryPort aporteRepository;
    private final RevisionRepository   revisionRepository;
    private final SaldoRepositoryPort  saldoRepository;

    @Override
    @Transactional
    public Aporte anular(AnularAporteCommand command) {
        Aporte aporte = aporteRepository.findById(command.aporteId())
                .orElseThrow(() -> new AporteNotFoundException(command.aporteId()));

        // Solo el afiliado dueño puede anular su propio aporte
        if (!aporte.getAfiliadoId().equals(command.afiliadoId())) {
            throw new AporteNoAutorizadoException(command.aporteId(), command.afiliadoId());
        }

        // Valida la transición — PENDIENTE → ANULADO (EN_REVISION lanza TransicionEstadoInvalidaException)
        aporte.getEstado().transicionar(EstadoAporte.ANULADO);

        Aporte anulado = new Aporte(aporte.getId(), aporte.getAfiliadoId(), aporte.getMonto(),
                aporte.getFecha(), aporte.getCanal(), aporte.getPeriodo(),
                EstadoAporte.ANULADO, aporte.getIdempotenciaKey());

        Aporte guardado = aporteRepository.guardar(anulado);

        revisionRepository.guardar(new RevisionAporte(null, command.aporteId(),
                command.afiliadoId(), EstadoAporte.ANULADO,
                command.motivo() != null ? command.motivo() : "Anulado por el afiliado",
                OffsetDateTime.now()));

        // Liberar el cupo mensual
        saldoRepository.findByAfiliadoIdAndMes(aporte.getAfiliadoId(), aporte.getPeriodo())
                .ifPresent(saldo -> saldoRepository.guardar(
                        saldo.conTotal(saldo.getTotal().subtract(aporte.getMonto()))
                ));

        return guardado;
    }
}
