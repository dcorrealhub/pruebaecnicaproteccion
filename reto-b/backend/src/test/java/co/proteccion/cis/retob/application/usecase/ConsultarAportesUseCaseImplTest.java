package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.ConsolidadoAportes;
import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultarAportesUseCaseImplTest {

    @Mock
    private AporteRepositoryPort aporteRepository;

    @InjectMocks
    private ConsultarAportesUseCaseImpl useCase;

    @Test
    void consultar_sinAportes_retornaConsolidadoConTotalCero() {
        var query = new ConsultarAportesUseCase.ConsultarAportesQuery("AF-001", "2026-01", "2026-06");
        when(aporteRepository.findByAfiliadoIdAndPeriodoBetween("AF-001", "2026-01", "2026-06"))
                .thenReturn(List.of());

        ConsolidadoAportes resultado = useCase.consultar(query);

        assertThat(resultado.totalAportado()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resultado.detalle()).isEmpty();
    }

    @Test
    void consultar_variosAportes_sumaCorrectamente() {
        var query = new ConsultarAportesUseCase.ConsultarAportesQuery("AF-001", "2026-01", "2026-06");
        List<Aporte> aportes = List.of(
                aporte("AF-001", new BigDecimal("1000000"), "2026-01"),
                aporte("AF-001", new BigDecimal("2000000"), "2026-02"),
                aporte("AF-001", new BigDecimal("500000"),  "2026-03")
        );
        when(aporteRepository.findByAfiliadoIdAndPeriodoBetween("AF-001", "2026-01", "2026-06"))
                .thenReturn(aportes);

        ConsolidadoAportes resultado = useCase.consultar(query);

        assertThat(resultado.totalAportado()).isEqualByComparingTo(new BigDecimal("3500000"));
        assertThat(resultado.detalle()).hasSize(3);
        assertThat(resultado.afiliadoId()).isEqualTo("AF-001");
    }

    private Aporte aporte(String afiliadoId, BigDecimal monto, String periodo) {
        return new Aporte(1L, afiliadoId, monto, LocalDate.now(), "WEB", periodo, false, "key-" + periodo);
    }
}
