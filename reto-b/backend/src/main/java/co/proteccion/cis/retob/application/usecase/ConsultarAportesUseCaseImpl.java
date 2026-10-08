package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.exception.SolicitudInvalidaException;
import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.ConsolidadoAportes;
import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Consulta del consolidado de aportes de un afiliado en un rango de periodos (inclusive).
 */
@Service
@RequiredArgsConstructor
public class ConsultarAportesUseCaseImpl implements ConsultarAportesUseCase {

    private final AporteRepositoryPort aporteRepository;

    @Override
    @Transactional(readOnly = true)
    public ConsolidadoAportes consultar(ConsultarAportesQuery query) {
        YearMonth desde = parsearPeriodo(query.periodoDesde(), "periodoDesde");
        YearMonth hasta = parsearPeriodo(query.periodoHasta(), "periodoHasta");
        if (desde.isAfter(hasta)) {
            throw new SolicitudInvalidaException("PERIODO_INVALIDO",
                    "periodoDesde no puede ser posterior a periodoHasta");
        }

        // YearMonth.toString() normaliza a YYYY-MM, el mismo formato persistido;
        // por eso la comparación lexicográfica BETWEEN es correcta.
        List<Aporte> detalle = aporteRepository.findByAfiliadoIdAndPeriodoBetween(
                query.afiliadoId(), desde.toString(), hasta.toString());

        BigDecimal total = detalle.stream()
                .map(Aporte::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ConsolidadoAportes(query.afiliadoId(), desde.toString(), hasta.toString(), total, detalle);
    }

    private static YearMonth parsearPeriodo(String valor, String campo) {
        try {
            return YearMonth.parse(valor);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new SolicitudInvalidaException("PERIODO_INVALIDO", campo + " debe tener formato YYYY-MM");
        }
    }
}
