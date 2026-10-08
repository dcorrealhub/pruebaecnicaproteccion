package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.exception.ConsultaInvalidaException;
import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.ConsolidadoAportes;
import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Implementación del caso de uso de consulta de aportes.
 *
 * <p>Valida que el rango de periodos sea coherente y no supere el máximo configurado,
 * y retorna el detalle de aportes del afiliado con su total.
 */
@Service
public class ConsultarAportesUseCaseImpl implements ConsultarAportesUseCase {

    private final AporteRepositoryPort aporteRepository;
    private final int rangoMaximoMeses;

    public ConsultarAportesUseCaseImpl(
            AporteRepositoryPort aporteRepository,
            @Value("${aporte.consulta-rango-maximo-meses}") int rangoMaximoMeses) {
        this.aporteRepository = aporteRepository;
        this.rangoMaximoMeses = rangoMaximoMeses;
    }

    @Override
    @Transactional(readOnly = true)
    public ConsolidadoAportes consultar(ConsultarAportesQuery query) {
        validarRango(query);

        List<Aporte> detalle = aporteRepository.findByAfiliadoIdAndPeriodoBetween(
                query.afiliadoId(), query.periodoDesde(), query.periodoHasta());
        BigDecimal total = detalle.stream()
                .map(Aporte::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ConsolidadoAportes(
                query.afiliadoId(), query.periodoDesde(), query.periodoHasta(), total, detalle);
    }

    private void validarRango(ConsultarAportesQuery query) {
        YearMonth desde = parsearPeriodo(query.periodoDesde());
        YearMonth hasta = parsearPeriodo(query.periodoHasta());

        if (desde.isAfter(hasta)) {
            throw new ConsultaInvalidaException("PERIODO_INVERTIDO",
                    "El periodo desde (" + desde + ") no puede ser posterior al periodo hasta (" + hasta + ")");
        }
        long meses = ChronoUnit.MONTHS.between(desde, hasta) + 1;
        if (meses > rangoMaximoMeses) {
            throw new ConsultaInvalidaException("RANGO_PERIODO_EXCEDIDO",
                    "El rango consultado (" + meses + " meses) supera el máximo permitido de "
                            + rangoMaximoMeses + " meses");
        }
    }

    private YearMonth parsearPeriodo(String periodo) {
        try {
            return YearMonth.parse(periodo);
        } catch (DateTimeParseException e) {
            throw new ConsultaInvalidaException("PERIODO_INVALIDO",
                    "El periodo '" + periodo + "' no tiene el formato YYYY-MM");
        }
    }
}
