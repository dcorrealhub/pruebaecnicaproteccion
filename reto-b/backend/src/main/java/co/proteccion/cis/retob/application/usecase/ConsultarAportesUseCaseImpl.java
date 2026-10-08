package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.exception.RecursoNoEncontradoException;
import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.ConsolidadoAportes;
import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Implementación del caso de uso de consulta de aportes (solo lectura — CQRS).
 */
@Service
public class ConsultarAportesUseCaseImpl implements ConsultarAportesUseCase {

    private final AporteRepositoryPort aporteRepository;

    public ConsultarAportesUseCaseImpl(AporteRepositoryPort aporteRepository) {
        this.aporteRepository = aporteRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ConsolidadoAportes consultar(ConsultarAportesQuery query) {
        List<Aporte> detalle = aporteRepository.findByAfiliadoIdAndPeriodoBetween(
                query.afiliadoId(), query.periodoDesde(), query.periodoHasta());

        BigDecimal total = detalle.stream()
                .map(Aporte::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_EVEN);

        return new ConsolidadoAportes(
                query.afiliadoId(),
                query.periodoDesde(),
                query.periodoHasta(),
                total,
                detalle);
    }

    @Override
    @Transactional(readOnly = true)
    public Aporte buscarPorId(Long id) {
        return aporteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un aporte con el id indicado."));
    }
}
