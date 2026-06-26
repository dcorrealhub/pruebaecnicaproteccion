package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.ConsolidadoAportes;
import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultarAportesUseCaseImpl implements ConsultarAportesUseCase {

    private final AporteRepositoryPort aporteRepository;

    @Override
    @Transactional(readOnly = true)
    public ConsolidadoAportes consultar(ConsultarAportesQuery query) {
        log.info("Consultando consolidado: afiliadoId={}, periodoDesde={}, periodoHasta={}",
                query.afiliadoId(), query.periodoDesde(), query.periodoHasta());

        var afiliadoId = query.afiliadoId();
        var desde = query.periodoDesde();
        var hasta = query.periodoHasta();

        if (afiliadoId == null || afiliadoId.isBlank()) {
            throw new IllegalArgumentException("El afiliadoId es obligatorio");
        }
        if (desde == null || hasta == null || desde.isBlank() || hasta.isBlank()) {
            throw new IllegalArgumentException("Los periodos desde y hasta son obligatorios");
        }
        if (desde.compareTo(hasta) > 0) {
            log.warn("Periodos invertidos, intercambiando: desde={}, hasta={}", desde, hasta);
            var temp = desde;
            desde = hasta;
            hasta = temp;
        }

        List<Aporte> detalle = aporteRepository.findByAfiliadoIdAndPeriodoBetween(
                afiliadoId, desde, hasta
        );

        BigDecimal total = detalle.stream()
                .map(Aporte::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Consolidado encontrado: afiliadoId={}, total={}, cantidad={}",
                afiliadoId, total, detalle.size());

        return new ConsolidadoAportes(
                afiliadoId, desde, hasta, total, detalle
        );
    }
}
