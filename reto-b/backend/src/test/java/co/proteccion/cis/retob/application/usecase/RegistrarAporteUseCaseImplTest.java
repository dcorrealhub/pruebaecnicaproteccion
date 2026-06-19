package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.application.AporteLimites;
import co.proteccion.cis.retob.domain.exception.ReglaNegocioException;
import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrarAporteUseCaseImplTest {

    @Mock
    private AporteRepositoryPort aporteRepository;

    @Mock
    private SaldoRepositoryPort saldoRepository;

    private RegistrarAporteUseCaseImpl useCase;

    private static final BigDecimal TOPE = new BigDecimal("10000000");
    private static final BigDecimal UMBRAL = new BigDecimal("5000000");
    // Clock fijo al 2026-06-19 para que los tests sean deterministas
    private static final Clock CLOCK_FIJO = Clock.fixed(
            Instant.parse("2026-06-19T10:00:00Z"), ZoneId.of("America/Bogota"));
    static final String PERIODO_TEST = "2026-06";

    @BeforeEach
    void configurar() {
        useCase = new RegistrarAporteUseCaseImpl(aporteRepository, saldoRepository,
                new AporteLimites(TOPE, UMBRAL), CLOCK_FIJO);
    }

    @Test
    void registrar_montoValido_persisteYRetornaAporte() {
        var command = comandoCon("AF-001", new BigDecimal("1000000"), "clave-1");
        var saldoVacio = saldoConTotal("AF-001", BigDecimal.ZERO);
        var aportePersistido = aporteConId(1L, command);

        when(aporteRepository.findByIdempotenciaKey("clave-1")).thenReturn(Optional.empty());
        when(saldoRepository.findByAfiliadoIdAndMes(eq("AF-001"), anyString())).thenReturn(Optional.of(saldoVacio));
        when(aporteRepository.guardar(any())).thenReturn(aportePersistido);
        when(saldoRepository.guardar(any())).thenReturn(saldoVacio);

        Aporte resultado = useCase.registrar(command);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.isMarcadaRevision()).isFalse();
        verify(aporteRepository).guardar(any());
        verify(saldoRepository).guardar(any());
    }

    @Test
    void registrar_montoSuperaUmbral_quedaMarcadaRevision() {
        var command = comandoCon("AF-001", new BigDecimal("6000000"), "clave-2");
        var saldoVacio = saldoConTotal("AF-001", BigDecimal.ZERO);
        var aportePersistido = aporteConId(2L, command);
        aportePersistido = new Aporte(2L, command.afiliadoId(), command.monto(),
                LocalDate.now(CLOCK_FIJO), command.canal(), PERIODO_TEST, true, command.idempotenciaKey());

        when(aporteRepository.findByIdempotenciaKey("clave-2")).thenReturn(Optional.empty());
        when(saldoRepository.findByAfiliadoIdAndMes(eq("AF-001"), anyString())).thenReturn(Optional.of(saldoVacio));
        when(aporteRepository.guardar(any())).thenReturn(aportePersistido);
        when(saldoRepository.guardar(any())).thenReturn(saldoVacio);

        Aporte resultado = useCase.registrar(command);

        assertThat(resultado.isMarcadaRevision()).isTrue();
        // Verificar que el aporte guardado tiene marcadaRevision = true
        ArgumentCaptor<Aporte> captor = ArgumentCaptor.forClass(Aporte.class);
        verify(aporteRepository).guardar(captor.capture());
        assertThat(captor.getValue().isMarcadaRevision()).isTrue();
    }

    @Test
    void registrar_superaTopeMensual_lanzaReglaNegocioException() {
        var command = comandoCon("AF-001", new BigDecimal("3000000"), "clave-3");
        // Saldo previo: 8.000.000 + 3.000.000 = 11.000.000 > tope
        var saldoConAcumulado = saldoConTotal("AF-001", new BigDecimal("8000000"));

        when(aporteRepository.findByIdempotenciaKey("clave-3")).thenReturn(Optional.empty());
        when(saldoRepository.findByAfiliadoIdAndMes(eq("AF-001"), anyString()))
                .thenReturn(Optional.of(saldoConAcumulado));

        assertThatThrownBy(() -> useCase.registrar(command))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("tope mensual");

        verify(aporteRepository, never()).guardar(any());
    }

    @Test
    void registrar_claveIdempotenteDuplicada_retornaAporteExistente() {
        var command = comandoCon("AF-001", new BigDecimal("1000000"), "clave-dup");
        var aporteExistente = aporteConId(99L, command);

        when(aporteRepository.findByIdempotenciaKey("clave-dup")).thenReturn(Optional.of(aporteExistente));

        Aporte resultado = useCase.registrar(command);

        assertThat(resultado.getId()).isEqualTo(99L);
        verify(saldoRepository, never()).findByAfiliadoIdAndMes(any(), any());
        verify(aporteRepository, never()).guardar(any());
    }

    @Test
    void registrar_sinSaldoPrevio_inicializaYPersiste() {
        var command = comandoCon("AF-002", new BigDecimal("500000"), "clave-4");
        var saldoNuevo = saldoConTotal("AF-002", BigDecimal.ZERO);

        when(aporteRepository.findByIdempotenciaKey("clave-4")).thenReturn(Optional.empty());
        when(saldoRepository.findByAfiliadoIdAndMes(eq("AF-002"), anyString())).thenReturn(Optional.empty());
        when(saldoRepository.inicializar(eq("AF-002"), anyString())).thenReturn(saldoNuevo);
        when(aporteRepository.guardar(any())).thenReturn(aporteConId(5L, command));
        when(saldoRepository.guardar(any())).thenReturn(saldoNuevo);

        useCase.registrar(command);

        verify(saldoRepository).inicializar(eq("AF-002"), anyString());
    }

    // ---- helpers ----

    private RegistrarAporteUseCase.RegistrarAporteCommand comandoCon(
            String afiliadoId, BigDecimal monto, String clave) {
        return new RegistrarAporteUseCase.RegistrarAporteCommand(afiliadoId, monto, "APP_MOVIL", clave);
    }

    private SaldoMensual saldoConTotal(String afiliadoId, BigDecimal total) {
        return new SaldoMensual(1L, afiliadoId, PERIODO_TEST, total, 0);
    }

    private Aporte aporteConId(Long id, RegistrarAporteUseCase.RegistrarAporteCommand cmd) {
        return new Aporte(id, cmd.afiliadoId(), cmd.monto(), LocalDate.now(CLOCK_FIJO),
                cmd.canal(), PERIODO_TEST, false, cmd.idempotenciaKey());
    }
}
