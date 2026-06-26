package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.ConsolidadoAportes;
import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultarAportesUseCaseImpl implements ConsultarAportesUseCase {

    private final AporteRepositoryPort aporteRepository;

    @Override
    @Transactional(readOnly = true)
    public ConsolidadoAportes consultar(ConsultarAportesQuery query) {
        log.info("Consultando consolidado: afiliadoId={}, periodoDesde={}, periodoHasta={}",
                query.afiliadoId(), query.periodoDesde(), query.periodoHasta());

        List<Aporte> detalle = aporteRepository.findByAfiliadoIdAndPeriodoBetween(
                query.afiliadoId(),
                query.periodoDesde(),
                query.periodoHasta()
        );

        BigDecimal total = detalle.stream()
                .map(Aporte::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Consolidado encontrado: afiliadoId={}, total={}, cantidad={}",
                query.afiliadoId(), total, detalle.size());

        return new ConsolidadoAportes(
                query.afiliadoId(),
                query.periodoDesde(),
                query.periodoHasta(),
                total,
                detalle
        );
    }
}
