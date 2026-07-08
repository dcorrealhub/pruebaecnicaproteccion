package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.model.ConsolidadoAportes;
import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ConsultarAportesUseCaseImpl implements ConsultarAportesUseCase {

    private final AporteRepositoryPort aporteRepository;

    @Override
    @Transactional(readOnly = true)
    public ConsolidadoAportes consultar(ConsultarAportesQuery query) {
        var detalle = aporteRepository.findByAfiliadoIdAndPeriodoBetween(
                query.afiliadoId(), query.periodoDesde(), query.periodoHasta());

        var total = detalle.stream()
                .map(aporte -> aporte.getMonto())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ConsolidadoAportes(
                query.afiliadoId(),
                query.periodoDesde(),
                query.periodoHasta(),
                total,
                detalle
        );
    }
}
