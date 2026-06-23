package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.model.ConsolidadoAportes;
import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.domain.service.AporteDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConsultarAportesUseCaseImpl implements ConsultarAportesUseCase {

    private final AporteRepositoryPort aporteRepository;
    private final AporteDomainService domainService;

    @Override
    @Transactional(readOnly = true)
    public ConsolidadoAportes consultar(ConsultarAportesQuery query) {
        String afiliadoId = query.afiliadoId().trim();
        String periodoDesde = query.periodoDesde().trim();
        String periodoHasta = query.periodoHasta().trim();

        domainService.validarRangoPeriodos(periodoDesde, periodoHasta);

        var resultado = aporteRepository.buscarConsolidado(afiliadoId, periodoDesde, periodoHasta);

        return new ConsolidadoAportes(
                afiliadoId,
                periodoDesde,
                periodoHasta,
                resultado.totalAportado(),
                resultado.aportes()
        );
    }
}
