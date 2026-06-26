package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase.ConsultarAportesQuery;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultarAportesUseCaseImplTest {

    @Mock AporteRepositoryPort aporteRepository;

    ConsultarAportesUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ConsultarAportesUseCaseImpl(aporteRepository);
    }

    // Cuando no hay aportes en el rango, el total debe ser cero y el detalle vacío (no nulo).
    @Test
    void consultar_sinAportes_retornaConsolidadoConTotalCero() {
        when(aporteRepository.findByAfiliadoIdAndPeriodoBetween("AF-001", "2025-01", "2025-06"))
                .thenReturn(List.of());

        var result = useCase.consultar(new ConsultarAportesQuery("AF-001", "2025-01", "2025-06"));

        assertThat(result.totalAportado()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.detalle()).isEmpty();
    }

    // Verifica que la suma acumula correctamente aportes de distintos periodos dentro del rango.
    @Test
    void consultar_conVariosAportes_sumaTotalCorrectamente() {
        var a1 = aporte("2025-01", new BigDecimal("1000000"));
        var a2 = aporte("2025-03", new BigDecimal("2500000"));
        var a3 = aporte("2025-06", new BigDecimal("500000"));
        when(aporteRepository.findByAfiliadoIdAndPeriodoBetween("AF-001", "2025-01", "2025-06"))
                .thenReturn(List.of(a1, a2, a3));

        var result = useCase.consultar(new ConsultarAportesQuery("AF-001", "2025-01", "2025-06"));

        assertThat(result.totalAportado()).isEqualByComparingTo(new BigDecimal("4000000"));
        assertThat(result.detalle()).hasSize(3);
    }

    // El consolidado debe reflejar exactamente los parámetros de búsqueda recibidos.
    @Test
    void consultar_retornaMetadatosDePeriodo() {
        when(aporteRepository.findByAfiliadoIdAndPeriodoBetween(any(), any(), any()))
                .thenReturn(List.of());

        var result = useCase.consultar(new ConsultarAportesQuery("AF-002", "2024-11", "2024-12"));

        assertThat(result.afiliadoId()).isEqualTo("AF-002");
        assertThat(result.periodoDesde()).isEqualTo("2024-11");
        assertThat(result.periodoHasta()).isEqualTo("2024-12");
    }

    // Un afiliadoId en minúsculas debe normalizarse antes de consultar el repositorio y en la respuesta.
    @Test
    void consultar_afiliadoIdEnMinusculas_normalizaAMayusculas() {
        when(aporteRepository.findByAfiliadoIdAndPeriodoBetween("AF-001", "2025-01", "2025-06"))
                .thenReturn(List.of(aporte("2025-03", new BigDecimal("1000000"))));

        var result = useCase.consultar(new ConsultarAportesQuery("af-001", "2025-01", "2025-06"));

        assertThat(result.afiliadoId()).isEqualTo("AF-001");
        assertThat(result.totalAportado()).isEqualByComparingTo(new BigDecimal("1000000"));
    }

    private Aporte aporte(String periodo, BigDecimal monto) {
        return new Aporte(null, "AF-001", monto, LocalDate.now(), "WEB", periodo, false, "KEY");
    }

    private static String any() { return org.mockito.ArgumentMatchers.any(); }
}
