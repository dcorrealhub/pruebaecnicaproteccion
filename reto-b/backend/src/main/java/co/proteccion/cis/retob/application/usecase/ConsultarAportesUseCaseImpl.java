package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.exception.ReglaNegocioException;
import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.ConsolidadoAportes;
import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultarAportesUseCaseImpl implements ConsultarAportesUseCase {

    private final AporteRepositoryPort aporteRepository;

    @Transactional(readOnly = true)
    @Override
    public ConsolidadoAportes consultar(ConsultarAportesQuery query) {
        if (query.periodoDesde().compareTo(query.periodoHasta()) > 0) {
            throw new ReglaNegocioException(
                    "RANGO_PERIODO_INVALIDO",
                    "El periodo de inicio (" + query.periodoDesde() + ") no puede ser posterior al periodo de fin (" + query.periodoHasta() + ")"
            );
        }

        List<Aporte> aportes = aporteRepository.findByAfiliadoIdAndPeriodoBetween(
                query.afiliadoId(), query.periodoDesde(), query.periodoHasta());

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
