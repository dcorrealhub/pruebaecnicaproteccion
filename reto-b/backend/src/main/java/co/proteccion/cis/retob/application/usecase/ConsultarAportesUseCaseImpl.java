package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.ConsolidadoAportes;
import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Implementación del caso de uso de consulta de aportes.
 *
 * TODO (candidato): implementar la consulta:
 *   1. Buscar los aportes del afiliado en el rango de periodos.
 *   2. Calcular el total sumando los montos (usar BigDecimal.add).
 *   3. Retornar el ConsolidadoAportes con total y detalle.
 *   4. Anotar como @Transactional(readOnly = true).
 */
@Service
@RequiredArgsConstructor
public class ConsultarAportesUseCaseImpl implements ConsultarAportesUseCase {

    private final AporteRepositoryPort aporteRepository;

    @Override
    @Transactional(readOnly = true)
    public ConsolidadoAportes consultar(ConsultarAportesQuery query) {
        List<Aporte> detalle = aporteRepository.findByAfiliadoIdAndPeriodoBetween(
                query.afiliadoId(),
                query.periodoDesde(),
                query.periodoHasta()
        );

        BigDecimal total = detalle.stream()
                .map(Aporte::getMonto)
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