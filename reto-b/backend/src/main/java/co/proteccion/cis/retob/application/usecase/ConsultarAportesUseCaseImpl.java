package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.ConsolidadoAportes;
import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultarAportesUseCaseImpl implements ConsultarAportesUseCase {

    private final AporteRepositoryPort aporteRepository;

    @Override
    @Transactional(readOnly = true)
    public ConsolidadoAportes consultar(ConsultarAportesQuery query) {
        String periodoActual = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        if (query.periodoHasta().compareTo(periodoActual) > 0) {
            throw new IllegalArgumentException("No se puede consultar un periodo futuro");
        }

        List<Aporte> aportes = aporteRepository.findByAfiliadoIdAndPeriodoBetween(
                query.afiliadoId(),
                query.periodoDesde(),
                query.periodoHasta()
        );

        BigDecimal total = aportes.stream()
                .map(Aporte::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ConsolidadoAportes(
                query.afiliadoId(),
                query.periodoDesde(),
                query.periodoHasta(),
                total,
                aportes
        );
    }
}
