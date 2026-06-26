package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.model.*;
import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase.ConsultarAportesQuery;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
@DisplayName("ConsultarAportesUseCaseImpl")
class ConsultarAportesUseCaseImplTest {

    @Mock AporteRepositoryPort aporteRepository;
    @InjectMocks ConsultarAportesUseCaseImpl useCase;

    private static final String AFILIADO  = "AF-001";
    private static final String DESDE     = "2026-01";
    private static final String HASTA     = "2026-06";

    private Aporte aporte(BigDecimal monto) {
        return new Aporte(null, AFILIADO, monto, LocalDate.now(),
                CanalOrigen.WEB, "2026-06", EstadoAporte.PENDIENTE, "k");
    }

    @Nested
    @DisplayName("Sin aportes en el periodo")
    class SinAportes {

        @Test
        @DisplayName("devuelve totalAportado = 0 y detalle vacío")
        void lista_vacia() {
            when(aporteRepository.findByAfiliadoIdAndPeriodoBetween(AFILIADO, DESDE, HASTA))
                    .thenReturn(List.of());

            ConsolidadoAportes resultado = useCase.consultar(new ConsultarAportesQuery(AFILIADO, DESDE, HASTA));

            assertThat(resultado.totalAportado()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(resultado.detalle()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Con aportes en el periodo")
    class ConAportes {

        @Test
        @DisplayName("suma correctamente con BigDecimal — sin pérdida de precisión")
        void suma_correcta() {
            List<Aporte> aportes = List.of(
                    aporte(new BigDecimal("1000000.50")),
                    aporte(new BigDecimal("2500000.25")),
                    aporte(new BigDecimal("500000.00"))
            );
            when(aporteRepository.findByAfiliadoIdAndPeriodoBetween(AFILIADO, DESDE, HASTA))
                    .thenReturn(aportes);

            ConsolidadoAportes resultado = useCase.consultar(new ConsultarAportesQuery(AFILIADO, DESDE, HASTA));

            assertThat(resultado.totalAportado()).isEqualByComparingTo(new BigDecimal("4000000.75"));
            assertThat(resultado.detalle()).hasSize(3);
        }

        @Test
        @DisplayName("retorna los metadatos del query (afiliadoId, periodoDesde, periodoHasta)")
        void metadatos_correctos() {
            when(aporteRepository.findByAfiliadoIdAndPeriodoBetween(AFILIADO, DESDE, HASTA))
                    .thenReturn(List.of(aporte(new BigDecimal("1000000"))));

            ConsolidadoAportes resultado = useCase.consultar(new ConsultarAportesQuery(AFILIADO, DESDE, HASTA));

            assertThat(resultado.afiliadoId()).isEqualTo(AFILIADO);
            assertThat(resultado.periodoDesde()).isEqualTo(DESDE);
            assertThat(resultado.periodoHasta()).isEqualTo(HASTA);
        }
    }
}
