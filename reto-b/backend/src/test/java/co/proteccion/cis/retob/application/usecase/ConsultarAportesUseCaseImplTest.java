package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase.ConsultarAportesQuery;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsultarAportesUseCaseImplTest {

    @Mock
    private AporteRepositoryPort aporteRepository;

    @InjectMocks
    private ConsultarAportesUseCaseImpl useCase;

    private final ConsultarAportesQuery query = new ConsultarAportesQuery("AF-001", "2026-01", "2026-06");

    @Test
    void consultar_cuandoExistenAportes_retornaConsolidadoConTotalYDetalle() {
        var a1 = crearAporte(1L, "AF-001", "100000", "2026-01-15");
        var a2 = crearAporte(2L, "AF-001", "200000", "2026-02-10");
        when(aporteRepository.findByAfiliadoIdAndPeriodoBetween("AF-001", "2026-01", "2026-06"))
                .thenReturn(List.of(a1, a2));

        var result = useCase.consultar(query);

        assertNotNull(result);
        assertEquals("AF-001", result.afiliadoId());
        assertEquals("2026-01", result.periodoDesde());
        assertEquals("2026-06", result.periodoHasta());
        assertEquals(0, new BigDecimal("300000").compareTo(result.totalAportado()));
        assertEquals(2, result.detalle().size());
        verify(aporteRepository).findByAfiliadoIdAndPeriodoBetween("AF-001", "2026-01", "2026-06");
    }

    @Test
    void consultar_cuandoNoHayAportes_retornaTotalCeroYListaVacia() {
        when(aporteRepository.findByAfiliadoIdAndPeriodoBetween(anyString(), anyString(), anyString()))
                .thenReturn(List.of());

        var result = useCase.consultar(query);

        assertNotNull(result);
        assertEquals(0, BigDecimal.ZERO.compareTo(result.totalAportado()));
        assertTrue(result.detalle().isEmpty());
    }

    @Test
    void consultar_conUnSoloAporte_retornaTotalCorrecto() {
        var a1 = crearAporte(1L, "AF-001", "500000", "2026-03-01");
        when(aporteRepository.findByAfiliadoIdAndPeriodoBetween(anyString(), anyString(), anyString()))
                .thenReturn(List.of(a1));

        var result = useCase.consultar(query);

        assertEquals(0, new BigDecimal("500000").compareTo(result.totalAportado()));
        assertEquals(1, result.detalle().size());
    }

    @Test
    void consultar_conPeriodosInvertidos_intercambiaYRetornaResultado() {
        var a1 = crearAporte(1L, "AF-001", "100000", "2026-03-01");
        var queryInvertida = new ConsultarAportesQuery("AF-001", "2026-06", "2026-01");
        when(aporteRepository.findByAfiliadoIdAndPeriodoBetween("AF-001", "2026-01", "2026-06"))
                .thenReturn(List.of(a1));

        var result = useCase.consultar(queryInvertida);

        assertEquals("2026-01", result.periodoDesde());
        assertEquals("2026-06", result.periodoHasta());
        assertEquals(1, result.detalle().size());
    }

    @Test
    void consultar_conAfiliadoNulo_lanzaExcepcion() {
        var query = new ConsultarAportesQuery(null, "2026-01", "2026-06");

        assertThrows(IllegalArgumentException.class, () -> useCase.consultar(query));
    }

    @Test
    void consultar_conPeriodosNulos_lanzaExcepcion() {
        var query = new ConsultarAportesQuery("AF-001", null, null);

        assertThrows(IllegalArgumentException.class, () -> useCase.consultar(query));
    }

    private Aporte crearAporte(Long id, String afiliadoId, String monto, String fecha) {
        return new Aporte(
                id,
                afiliadoId,
                new BigDecimal(monto),
                LocalDate.parse(fecha),
                "APP_MOVIL",
                "2026-01",
                false,
                "key-" + id
        );
    }
}