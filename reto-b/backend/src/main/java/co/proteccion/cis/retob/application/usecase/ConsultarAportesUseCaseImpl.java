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
 * Responsabilidad: recuperar los aportes de un afiliado en un rango de
 * periodos, calcular el total acumulado y retornar el consolidado.
 * No modifica estado — solo lectura.
 */
@Service
@RequiredArgsConstructor
public class ConsultarAportesUseCaseImpl implements ConsultarAportesUseCase {

    private final AporteRepositoryPort aporteRepository;

    /**
     * Retorna el consolidado de aportes de un afiliado en el rango de periodos
     * indicado en el {@code query}.
     *
     * <p>Pasos:
     * <ol>
     *   <li>Recuperar los aportes del afiliado cuyo periodo esté dentro del
     *       rango [{@code periodoDesde}, {@code periodoHasta}].</li>
     *   <li>Sumar los montos con {@code BigDecimal} para preservar precisión
     *       financiera. Una lista vacía produce total {@code 0}.</li>
     *   <li>Construir y retornar {@link ConsolidadoAportes} con el detalle y
     *       el total calculado.</li>
     * </ol>
     *
     * <p>{@code @Transactional(readOnly = true)} indica a Hibernate que no
     * realice dirty checking al cerrar la sesión, reduciendo overhead de
     * memoria y CPU en consultas con listas grandes.
     *
     * @param query parámetros de búsqueda: afiliadoId, periodoDesde, periodoHasta
     * @return consolidado con totalAportado y lista de aportes del periodo
     */
    @Override
    @Transactional(readOnly = true)
    public ConsolidadoAportes consultar(ConsultarAportesQuery query) {

        // 1. Recuperar aportes del rango de periodos solicitado
        List<Aporte> aportes = aporteRepository.findByAfiliadoIdAndPeriodoBetween(
                query.afiliadoId(),
                query.periodoDesde(),
                query.periodoHasta()
        );

        // 2. Sumar montos con BigDecimal para evitar pérdida de precisión
        //    BigDecimal.ZERO como identidad garantiza total=0 cuando la lista está vacía
        BigDecimal totalAportado = aportes.stream()
                .map(Aporte::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Construir y retornar el consolidado con detalle completo
        return new ConsolidadoAportes(
                query.afiliadoId(),
                query.periodoDesde(),
                query.periodoHasta(),
                totalAportado,
                aportes
        );
    }
}
