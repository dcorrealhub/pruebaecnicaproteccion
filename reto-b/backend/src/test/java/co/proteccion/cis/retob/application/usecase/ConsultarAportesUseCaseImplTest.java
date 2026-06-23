package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.exception.PeriodoInvalidoException;
import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.CanalAporte;
import co.proteccion.cis.retob.domain.model.ConsolidadoAportes;
import co.proteccion.cis.retob.domain.model.EstadoAporte;
import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase.ConsultarAportesQuery;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort.ConsolidadoConsulta;
import co.proteccion.cis.retob.domain.service.AporteDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultarAportesUseCaseImplTest {

    @Mock
    private AporteRepositoryPort aporteRepository;

    private ConsultarAportesUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ConsultarAportesUseCaseImpl(aporteRepository, new AporteDomainService());
    }

    @Test
    void consultar_retornaConsolidadoConTotalYDetalle() {
        Aporte aporte1 = aporte(1L, new BigDecimal("100000"));
        Aporte aporte2 = aporte(2L, new BigDecimal("250000"));
        when(aporteRepository.buscarConsolidado("AF-001", "2026-01", "2026-06"))
                .thenReturn(new ConsolidadoConsulta(new BigDecimal("350000"), List.of(aporte1, aporte2)));

        ConsolidadoAportes result = useCase.consultar(new ConsultarAportesQuery("AF-001", "2026-01", "2026-06"));

        assertEquals("AF-001", result.afiliadoId());
        assertEquals("2026-01", result.periodoDesde());
        assertEquals("2026-06", result.periodoHasta());
        assertEquals(new BigDecimal("350000"), result.totalAportado());
        assertEquals(2, result.detalle().size());
        verify(aporteRepository).buscarConsolidado("AF-001", "2026-01", "2026-06");
    }

    @Test
    void consultar_sinAportes_retornaTotalCero() {
        when(aporteRepository.buscarConsolidado("AF-002", "2026-06", "2026-06"))
                .thenReturn(new ConsolidadoConsulta(BigDecimal.ZERO, List.of()));

        ConsolidadoAportes result = useCase.consultar(new ConsultarAportesQuery("AF-002", "2026-06", "2026-06"));

        assertEquals(BigDecimal.ZERO, result.totalAportado());
        assertEquals(0, result.detalle().size());
    }

    @Test
    void consultar_periodoDesdePosterior_lanzaExcepcion() {
        assertThrows(PeriodoInvalidoException.class,
                () -> useCase.consultar(new ConsultarAportesQuery("AF-003", "2026-12", "2026-01")));
    }

    @Test
    void consultar_periodoInvalido_lanzaExcepcion() {
        assertThrows(PeriodoInvalidoException.class,
                () -> useCase.consultar(new ConsultarAportesQuery("AF-003", "2026-13", "2026-06")));
    }

    private Aporte aporte(Long id, BigDecimal monto) {
        return new Aporte(
                id,
                "AF-001",
                monto,
                LocalDate.of(2026, 6, 15),
                CanalAporte.WEB,
                "2026-06",
                EstadoAporte.REGISTRADO,
                "KEY-" + id
        );
    }
}
