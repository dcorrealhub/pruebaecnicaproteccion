package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.model.ConsolidadoAportes;
import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
    public ConsolidadoAportes consultar(ConsultarAportesQuery query) {
        // TODO: implementar
        throw new UnsupportedOperationException("Pendiente de implementación");
    }
}
