package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.model.ConsolidadoAportes;
import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase.ConsultarAportesQuery;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.support.AporteMother;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ConsultarAportesUseCaseImplTest {

    @Mock
    AporteRepositoryPort aporteRepository;

    @InjectMocks
    ConsultarAportesUseCaseImpl useCase;

    private ConsultarAportesQuery query(String desde, String hasta) {
        return new ConsultarAportesQuery("AF-001", desde, hasta);
    }

    @Test
    void consultar_sinAportesEnPeriodo_retornaConsolidadoConTotalCeroYListaVacia() {
        given(aporteRepository.findByAfiliadoIdAndPeriodoBetween(anyString(), anyString(), anyString()))
                .willReturn(List.of());

        ConsolidadoAportes resultado = useCase.consultar(query("2026-01", "2026-06"));

        assertThat(resultado.totalAportado()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resultado.detalle()).isEmpty();
    }

    @Test
    void consultar_conVariosAportes_sumaTotalesCorrectamente() {
        var a1 = AporteMother.normal();                                               // 100000
        var a2 = AporteMother.normal();                                               // 100000
        var a3 = AporteMother.granMonto();                                            // 6000000
        given(aporteRepository.findByAfiliadoIdAndPeriodoBetween(anyString(), anyString(), anyString()))
                .willReturn(List.of(a1, a2, a3));

        ConsolidadoAportes resultado = useCase.consultar(query("2026-01", "2026-12"));

        assertThat(resultado.totalAportado()).isEqualByComparingTo("6200000.00");
    }

    @Test
    void consultar_retornaExactamenteLosAportesDelRepositorio() {
        given(aporteRepository.findByAfiliadoIdAndPeriodoBetween(anyString(), anyString(), anyString()))
                .willReturn(List.of(AporteMother.normal(), AporteMother.granMonto()));

        ConsolidadoAportes resultado = useCase.consultar(query("2026-01", "2026-06"));

        assertThat(resultado.detalle()).hasSize(2);
    }

    @Test
    void consultar_propagaElPeriodoDelQuery() {
        given(aporteRepository.findByAfiliadoIdAndPeriodoBetween(anyString(), anyString(), anyString()))
                .willReturn(List.of());

        ConsolidadoAportes resultado = useCase.consultar(query("2026-01", "2026-06"));

        assertThat(resultado.periodoDesde()).isEqualTo("2026-01");
        assertThat(resultado.periodoHasta()).isEqualTo("2026-06");
    }
}
