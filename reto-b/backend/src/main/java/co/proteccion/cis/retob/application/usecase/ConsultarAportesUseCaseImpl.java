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
import java.util.regex.Pattern;

/**
 * Implementación del caso de uso de consulta de aportes.
 * Devuelve el total aportado y el detalle de un afiliado en un rango de periodos (YYYY-MM).
 * Para un único periodo basta con periodoDesde == periodoHasta.
 */
@Service
@RequiredArgsConstructor
public class ConsultarAportesUseCaseImpl implements ConsultarAportesUseCase {

    private static final Pattern FORMATO_PERIODO = Pattern.compile("\\d{4}-(0[1-9]|1[0-2])");

    private final AporteRepositoryPort aporteRepository;

    @Override
    @Transactional(readOnly = true)
    public ConsolidadoAportes consultar(ConsultarAportesQuery query) {
        if (query.afiliadoId() == null || query.afiliadoId().isBlank()) {
            throw new IllegalArgumentException("El afiliadoId es obligatorio");
        }
        validarPeriodo(query.periodoDesde(), "periodoDesde");
        validarPeriodo(query.periodoHasta(), "periodoHasta");
        // YYYY-MM se ordena lexicográficamente igual que cronológicamente
        if (query.periodoDesde().compareTo(query.periodoHasta()) > 0) {
            throw new IllegalArgumentException("periodoDesde no puede ser posterior a periodoHasta");
        }

        List<Aporte> detalle = aporteRepository.findByAfiliadoIdAndPeriodoBetween(
                query.afiliadoId(), query.periodoDesde(), query.periodoHasta());

        BigDecimal total = detalle.stream()
                .map(Aporte::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ConsolidadoAportes(query.afiliadoId(), query.periodoDesde(),
                query.periodoHasta(), total, detalle);
    }

    private void validarPeriodo(String periodo, String campo) {
        if (periodo == null || !FORMATO_PERIODO.matcher(periodo).matches()) {
            throw new IllegalArgumentException(campo + " debe tener formato yyyy-MM");
        }
    }
}
