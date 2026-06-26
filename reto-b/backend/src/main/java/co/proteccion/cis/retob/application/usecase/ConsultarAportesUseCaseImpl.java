package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.ConsolidadoAportes;
import co.proteccion.cis.retob.domain.model.EstadoAporte;
import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Caso de uso de consulta del consolidado de aportes de un afiliado en un rango de periodos.
 *
 * <p>El total reportado suma solo los aportes {@code APROBADO} (los que cuentan para el tope);
 * los pendientes de revisión se totalizan aparte, y el detalle incluye todos con su estado.
 */
@Service
@RequiredArgsConstructor
public class ConsultarAportesUseCaseImpl implements ConsultarAportesUseCase {

    private final AporteRepositoryPort aporteRepository;

    @Override
    @Transactional(readOnly = true)
    public ConsolidadoAportes consultar(ConsultarAportesQuery query) {
        List<Aporte> detalle = aporteRepository.findByAfiliadoIdAndPeriodoBetween(
                query.afiliadoId(), query.periodoDesde(), query.periodoHasta());

        BigDecimal totalAprobado = sumarPorEstado(detalle, EstadoAporte.APROBADO);
        BigDecimal totalEnRevision = sumarPorEstado(detalle, EstadoAporte.PENDIENTE_REVISION);

        return new ConsolidadoAportes(
                query.afiliadoId(),
                query.periodoDesde(),
                query.periodoHasta(),
                totalAprobado,
                totalEnRevision,
                detalle);
    }

    private BigDecimal sumarPorEstado(List<Aporte> aportes, EstadoAporte estado) {
        return aportes.stream()
                .filter(a -> a.getEstado() == estado)
                .map(Aporte::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
