package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.ConsolidadoAportes;
import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase.ConsultarAportesQuery;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ConsultarAportesUseCaseImplTest {

    private final AporteRepositoryPort aporteRepository = mock(AporteRepositoryPort.class);
    private final ConsultarAportesUseCaseImpl useCase = new ConsultarAportesUseCaseImpl(aporteRepository);

    private Aporte aporte(long id, String monto) {
        return new Aporte(id, "AF-001", new BigDecimal(monto), LocalDate.of(2026, 10, 1),
                "WEB", "2026-10", false, "k-" + id, false);
    }

    @Test
    void sumaElTotalYDevuelveElDetalle() {
        when(aporteRepository.findByAfiliadoIdAndPeriodoBetween("AF-001", "2026-10", "2026-10"))
                .thenReturn(List.of(aporte(1, "1500000.50"), aporte(2, "250000")));

        ConsolidadoAportes resultado = useCase.consultar(new ConsultarAportesQuery("AF-001", "2026-10", "2026-10"));

        assertThat(resultado.totalAportado()).isEqualByComparingTo("1750000.50");
        assertThat(resultado.detalle()).hasSize(2);
    }

    @Test
    void sinAportesDevuelveTotalCero() {
        when(aporteRepository.findByAfiliadoIdAndPeriodoBetween(any(), any(), any())).thenReturn(List.of());

        var resultado = useCase.consultar(new ConsultarAportesQuery("AF-001", "2026-10", "2026-10"));

        assertThat(resultado.totalAportado()).isEqualByComparingTo("0");
        assertThat(resultado.detalle()).isEmpty();
    }

    @Test
    void rechazaPeriodoConFormatoInvalido() {
        assertThatThrownBy(() -> useCase.consultar(new ConsultarAportesQuery("AF-001", "2026-13", "2026-10")))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(aporteRepository);
    }
}
