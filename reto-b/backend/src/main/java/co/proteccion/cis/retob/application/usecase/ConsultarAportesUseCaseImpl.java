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

@Service
@RequiredArgsConstructor
public class ConsultarAportesUseCaseImpl implements ConsultarAportesUseCase {

    private final AporteRepositoryPort aporteRepository;

    @Override
    @Transactional(readOnly = true) // Indica a JPA que no es necesario rastrear cambios en las entidades.
    public ConsolidadoAportes consultar(ConsultarAportesQuery query) {
        // Normalizar a mayúsculas para que "af-001" y "AF-001" sean el mismo afiliado en toda la capa.
        String afiliadoId = query.afiliadoId().toUpperCase();

        // BETWEEN sobre YYYY-MM funciona correctamente en orden lexicográfico.
        List<Aporte> aportes = aporteRepository.findByAfiliadoIdAndPeriodoBetween(
                afiliadoId, query.periodoDesde(), query.periodoHasta());

        // BigDecimal.ZERO como identidad evita NPE cuando la lista está vacía.
        BigDecimal total = aportes.stream()
                .map(Aporte::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ConsolidadoAportes(afiliadoId, query.periodoDesde(), query.periodoHasta(), total, aportes);
    }
}
