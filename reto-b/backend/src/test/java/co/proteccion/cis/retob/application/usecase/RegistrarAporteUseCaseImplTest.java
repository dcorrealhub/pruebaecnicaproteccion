package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.application.config.AporteProperties;
import co.proteccion.cis.retob.application.service.IdempotenciaAporteService;
import co.proteccion.cis.retob.domain.exception.ConcurrenciaSaldoException;
import co.proteccion.cis.retob.domain.exception.MontoInvalidoException;
import co.proteccion.cis.retob.domain.exception.TopeMensualExcedidoException;
import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.CanalAporte;
import co.proteccion.cis.retob.domain.model.EstadoAporte;
import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.domain.model.TipoEventoAporte;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase.RegistrarAporteCommand;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.ClockPort;
import co.proteccion.cis.retob.domain.port.out.EventoAporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
import co.proteccion.cis.retob.domain.service.AporteDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrarAporteUseCaseImplTest {

    private static final LocalDate HOY = LocalDate.of(2026, 6, 15);
    private static final AporteProperties PROPERTIES = new AporteProperties(
            new BigDecimal("10000000"),
            new BigDecimal("5000000")
    );

    @Mock
    private AporteRepositoryPort aporteRepository;
    @Mock
    private SaldoRepositoryPort saldoRepository;
    @Mock
    private EventoAporteRepositoryPort eventoRepository;
    @Mock
    private ClockPort clock;
    @Mock
    private IdempotenciaAporteService idempotenciaService;

    private RegistrarAporteUseCaseImpl useCase;

    private AporteDomainService domainService;

    @BeforeEach
    void setUp() {
        domainService = new AporteDomainService();
        useCase = new RegistrarAporteUseCaseImpl(
                aporteRepository,
                saldoRepository,
                eventoRepository,
                clock,
                domainService,
                PROPERTIES,
                idempotenciaService
        );
        when(clock.hoy()).thenReturn(HOY);
    }

    @Test
    void registrar_idempotenciaExistente_retornaAporteSinDuplicar() {
        Aporte existente = aportePersistido("KEY-001", new BigDecimal("100000"), EstadoAporte.REGISTRADO);
        when(idempotenciaService.resolverAporteExistente("KEY-001")).thenReturn(Optional.of(existente));

        Aporte result = useCase.registrar(command("AF-001", new BigDecimal("100000"), "KEY-001"));

        assertEquals(existente, result);
        verify(idempotenciaService, never()).intentarClaim(any());
        verify(aporteRepository, never()).guardar(any());
        verify(saldoRepository, never()).guardar(any());
    }

    @Test
    void registrar_montoNegativo_lanzaExcepcion() {
        when(idempotenciaService.resolverAporteExistente("KEY-002")).thenReturn(Optional.empty());
        when(idempotenciaService.intentarClaim("KEY-002")).thenReturn(true);

        assertThrows(MontoInvalidoException.class,
                () -> useCase.registrar(command("AF-001", new BigDecimal("-1"), "KEY-002")));

        verify(idempotenciaService).liberarClaim("KEY-002");
    }

    @Test
    void registrar_excedeTopeMensual_lanzaExcepcion() {
        when(idempotenciaService.resolverAporteExistente("KEY-003")).thenReturn(Optional.empty());
        when(idempotenciaService.intentarClaim("KEY-003")).thenReturn(true);
        when(saldoRepository.findByAfiliadoIdAndMes("AF-001", "2026-06"))
                .thenReturn(Optional.of(new SaldoMensual(1L, "AF-001", "2026-06", new BigDecimal("9500000"), 0)));

        assertThrows(TopeMensualExcedidoException.class,
                () -> useCase.registrar(command("AF-001", new BigDecimal("600000"), "KEY-003")));

        verify(idempotenciaService).liberarClaim("KEY-003");
    }

    @Test
    void registrar_montoSuperaUmbral_marcaRevision() {
        when(idempotenciaService.resolverAporteExistente("KEY-004")).thenReturn(Optional.empty());
        when(idempotenciaService.intentarClaim("KEY-004")).thenReturn(true);
        when(saldoRepository.findByAfiliadoIdAndMes("AF-002", "2026-06")).thenReturn(Optional.empty());
        when(saldoRepository.inicializar("AF-002", "2026-06"))
                .thenReturn(new SaldoMensual(1L, "AF-002", "2026-06", BigDecimal.ZERO, 0));
        when(aporteRepository.guardar(any())).thenAnswer(invocation -> {
            Aporte aporte = invocation.getArgument(0);
            return aporte.conId(10L);
        });
        when(saldoRepository.guardar(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Aporte result = useCase.registrar(command("AF-002", new BigDecimal("6000000"), "KEY-004"));

        assertEquals(EstadoAporte.REQUIERE_REVISION, result.getEstado());
        verify(eventoRepository).registrar(10L, TipoEventoAporte.APORTE_REGISTRADO);
        verify(idempotenciaService).completar("KEY-004", 10L);
    }

    @Test
    void registrar_exitoso_actualizaSaldoYPersiste() {
        when(idempotenciaService.resolverAporteExistente("KEY-005")).thenReturn(Optional.empty());
        when(idempotenciaService.intentarClaim("KEY-005")).thenReturn(true);
        when(saldoRepository.findByAfiliadoIdAndMes("AF-003", "2026-06"))
                .thenReturn(Optional.of(new SaldoMensual(2L, "AF-003", "2026-06", new BigDecimal("1000000"), 1)));
        when(aporteRepository.guardar(any())).thenAnswer(invocation -> {
            Aporte aporte = invocation.getArgument(0);
            return aporte.conId(20L);
        });
        when(saldoRepository.guardar(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Aporte result = useCase.registrar(command("AF-003", new BigDecimal("500000"), "KEY-005"));

        assertEquals(EstadoAporte.REGISTRADO, result.getEstado());
        assertEquals(new BigDecimal("500000"), result.getMonto());
        verify(saldoRepository).guardar(any(SaldoMensual.class));
        verify(idempotenciaService).completar("KEY-005", 20L);
    }

    @Test
    void registrar_conflictoConcurrencia_reintentaHastaAgotar() {
        when(idempotenciaService.resolverAporteExistente("KEY-006")).thenReturn(Optional.empty());
        when(idempotenciaService.intentarClaim("KEY-006")).thenReturn(true);
        when(saldoRepository.findByAfiliadoIdAndMes("AF-004", "2026-06")).thenReturn(Optional.empty());
        when(saldoRepository.inicializar("AF-004", "2026-06"))
                .thenReturn(new SaldoMensual(3L, "AF-004", "2026-06", BigDecimal.ZERO, 0));
        when(aporteRepository.guardar(any())).thenAnswer(invocation -> {
            Aporte aporte = invocation.getArgument(0);
            return aporte.conId(30L);
        });
        when(saldoRepository.guardar(any()))
                .thenThrow(new ConcurrenciaSaldoException("AF-004", "2026-06"));

        assertThrows(ConcurrenciaSaldoException.class,
                () -> useCase.registrar(command("AF-004", new BigDecimal("100000"), "KEY-006")));

        verify(idempotenciaService).liberarClaim("KEY-006");
        verify(idempotenciaService, never()).completar(eq("KEY-006"), any());
    }

    private RegistrarAporteCommand command(String afiliadoId, BigDecimal monto, String key) {
        return new RegistrarAporteCommand(afiliadoId, monto, "WEB", key);
    }

    private Aporte aportePersistido(String key, BigDecimal monto, EstadoAporte estado) {
        return new Aporte(
                1L, "AF-001", monto, HOY, CanalAporte.WEB, "2026-06", estado, key);
    }
}
