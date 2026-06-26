package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.EstadoAporte;
import co.proteccion.cis.retob.domain.model.ParametrosAporte;
import co.proteccion.cis.retob.domain.model.ReglaNegocioException;
import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase.RegistrarAporteCommand;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.EventoAportePort;
import co.proteccion.cis.retob.domain.port.out.ParametroAportePort;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RegistrarAporteUseCaseImplTest {

    private static final BigDecimal TOPE = new BigDecimal("10000000");
    private static final BigDecimal UMBRAL = new BigDecimal("5000000");

    @Mock AporteRepositoryPort aporteRepository;
    @Mock SaldoRepositoryPort saldoRepository;
    @Mock ParametroAportePort parametroPort;
    @Mock EventoAportePort eventoPort;
    @Mock PlatformTransactionManager transactionManager;

    RegistrarAporteUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegistrarAporteUseCaseImpl(
                aporteRepository, saldoRepository, parametroPort, eventoPort, transactionManager);
        when(parametroPort.forAfiliado(anyString())).thenReturn(new ParametrosAporte(TOPE, UMBRAL));
        when(aporteRepository.findByIdempotenciaKey(anyString())).thenReturn(Optional.empty());
        when(aporteRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(saldoRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private RegistrarAporteCommand comando(String monto, String key) {
        return new RegistrarAporteCommand("AF-001", new BigDecimal(monto), LocalDate.of(2025, 6, 10), "WEB", key);
    }

    @Test
    void idempotencia_siExisteRetornaSinReprocesar() {
        Aporte existente = Aporte.nuevo("AF-001", new BigDecimal("100"), LocalDate.of(2025, 6, 1),
                "WEB", EstadoAporte.APROBADO, "k-dup");
        when(aporteRepository.findByIdempotenciaKey("k-dup")).thenReturn(Optional.of(existente));

        Aporte resultado = useCase.registrar(comando("999", "k-dup"));

        assertThat(resultado).isSameAs(existente);
        verify(aporteRepository, never()).guardar(any());
        verify(saldoRepository, never()).guardar(any());
    }

    @Test
    void montoSuperaUmbral_quedaPendientePeroReservaElTope() {
        when(saldoRepository.findByAfiliadoIdAndMes("AF-001", "2025-06"))
                .thenReturn(Optional.of(new SaldoMensual(1L, "AF-001", "2025-06", BigDecimal.ZERO, 0)));

        Aporte resultado = useCase.registrar(comando("6000000", "k1"));

        assertThat(resultado.getEstado()).isEqualTo(EstadoAporte.PENDIENTE_REVISION);
        assertThat(resultado.isMarcadaRevision()).isTrue();
        // El pendiente reserva cupo: el saldo se incrementa igual que un aprobado.
        ArgumentCaptor<SaldoMensual> captor = ArgumentCaptor.forClass(SaldoMensual.class);
        verify(saldoRepository).guardar(captor.capture());
        assertThat(captor.getValue().getTotal()).isEqualByComparingTo("6000000");
        verify(eventoPort).registrar(any(), org.mockito.ArgumentMatchers.eq(EventoAportePort.Tipo.APORTE_MARCADO_REVISION));
    }

    @Test
    void pendienteQueSuperaTope_seRechaza() {
        // Saldo en 5M, aporte 6M (supera umbral => pendiente) => 11M > tope 10M => rechazo.
        when(saldoRepository.findByAfiliadoIdAndMes("AF-001", "2025-06"))
                .thenReturn(Optional.of(new SaldoMensual(1L, "AF-001", "2025-06", new BigDecimal("5000000"), 0)));

        assertThatThrownBy(() -> useCase.registrar(comando("6000000", "k1")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("tope mensual");
        verify(aporteRepository, never()).guardar(any());
    }

    @Test
    void montoDentroDeTope_seApruebaEIncrementaElSaldo() {
        when(saldoRepository.findByAfiliadoIdAndMes("AF-001", "2025-06"))
                .thenReturn(Optional.of(new SaldoMensual(1L, "AF-001", "2025-06", BigDecimal.ZERO, 0)));

        Aporte resultado = useCase.registrar(comando("4000000", "k1"));

        assertThat(resultado.getEstado()).isEqualTo(EstadoAporte.APROBADO);
        ArgumentCaptor<SaldoMensual> captor = ArgumentCaptor.forClass(SaldoMensual.class);
        verify(saldoRepository).guardar(captor.capture());
        assertThat(captor.getValue().getTotal()).isEqualByComparingTo("4000000");
    }

    @Test
    void acumuladoSuperaTope_seRechazaYNoGuardaAporte() {
        when(saldoRepository.findByAfiliadoIdAndMes("AF-001", "2025-06"))
                .thenReturn(Optional.of(new SaldoMensual(1L, "AF-001", "2025-06", new BigDecimal("8000000"), 0)));

        assertThatThrownBy(() -> useCase.registrar(comando("4000000", "k1")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("tope mensual");

        verify(aporteRepository, never()).guardar(any());
    }

    @Test
    void montoNoPositivo_lanzaValidacionDeDominio() {
        assertThatThrownBy(() -> useCase.registrar(comando("0", "k1")))
                .isInstanceOf(IllegalArgumentException.class);
        verify(aporteRepository, never()).guardar(any());
    }
}
