package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase.RegistrarAporteCommand;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.EventoAporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrarAporteUseCaseTest {

    @Mock AporteRepositoryPort aporteRepository;
    @Mock SaldoRepositoryPort saldoRepository;
    @Mock EventoAporteRepositoryPort eventoRepository;

    @InjectMocks
    RegistrarAporteUseCaseImpl useCase;

    private static final String PERIODO_ACTUAL =
            LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(useCase, "topeMensual", new BigDecimal("10000000"));
        ReflectionTestUtils.setField(useCase, "umbralRevision", new BigDecimal("5000000"));
    }

    @Test
    void registrar_happy_path() {
        SaldoMensual saldoVacio = new SaldoMensual(1L, "AF-001", PERIODO_ACTUAL, BigDecimal.ZERO, 0);
        Aporte aporteGuardado = aporte(1L, "AF-001", new BigDecimal("1000000"), false);

        when(aporteRepository.findByIdempotenciaKey("key-1")).thenReturn(Optional.empty());
        when(saldoRepository.findByAfiliadoIdAndMes("AF-001", PERIODO_ACTUAL))
                .thenReturn(Optional.of(saldoVacio));
        when(saldoRepository.guardar(any())).thenReturn(saldoVacio);
        when(aporteRepository.guardar(any())).thenReturn(aporteGuardado);

        Aporte result = useCase.registrar(command("AF-001", "1000000", "key-1"));

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.isMarcadaRevision()).isFalse();
        verify(eventoRepository).registrarEvento(1L, "APORTE_REGISTRADO");
    }

    @Test
    void registrar_idempotente_devuelve_existente() {
        Aporte existente = aporte(5L, "AF-001", new BigDecimal("500000"), false);
        when(aporteRepository.findByIdempotenciaKey("key-dup")).thenReturn(Optional.of(existente));

        Aporte result = useCase.registrar(command("AF-001", "500000", "key-dup"));

        assertThat(result.getId()).isEqualTo(5L);
        verifyNoInteractions(saldoRepository, eventoRepository);
    }

    @Test
    void registrar_marca_revision_cuando_supera_umbral() {
        SaldoMensual saldo = new SaldoMensual(1L, "AF-001", PERIODO_ACTUAL, BigDecimal.ZERO, 0);
        Aporte aporteGuardado = aporte(2L, "AF-001", new BigDecimal("6000000"), true);

        when(aporteRepository.findByIdempotenciaKey(anyString())).thenReturn(Optional.empty());
        when(saldoRepository.findByAfiliadoIdAndMes(anyString(), anyString()))
                .thenReturn(Optional.of(saldo));
        when(saldoRepository.guardar(any())).thenReturn(saldo);
        when(aporteRepository.guardar(any())).thenReturn(aporteGuardado);

        Aporte result = useCase.registrar(command("AF-001", "6000000", "key-2"));

        assertThat(result.isMarcadaRevision()).isTrue();
    }

    @Test
    void registrar_rechaza_cuando_supera_tope_mensual() {
        SaldoMensual saldo = new SaldoMensual(1L, "AF-001", PERIODO_ACTUAL, new BigDecimal("9000000"), 0);

        when(aporteRepository.findByIdempotenciaKey(anyString())).thenReturn(Optional.empty());
        when(saldoRepository.findByAfiliadoIdAndMes(anyString(), anyString()))
                .thenReturn(Optional.of(saldo));

        assertThatThrownBy(() -> useCase.registrar(command("AF-001", "2000000", "key-3")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tope mensual");

        verify(aporteRepository, never()).guardar(any());
        verifyNoInteractions(eventoRepository);
    }

    @Test
    void registrar_rechaza_monto_cero() {
        when(aporteRepository.findByIdempotenciaKey(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.registrar(command("AF-001", "0", "key-4")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mayor a cero");
    }

    @Test
    void registrar_inicializa_saldo_si_no_existe() {
        SaldoMensual saldoNuevo = new SaldoMensual(10L, "AF-002", PERIODO_ACTUAL, BigDecimal.ZERO, 0);
        Aporte aporteGuardado = aporte(3L, "AF-002", new BigDecimal("500000"), false);

        when(aporteRepository.findByIdempotenciaKey(anyString())).thenReturn(Optional.empty());
        when(saldoRepository.findByAfiliadoIdAndMes("AF-002", PERIODO_ACTUAL))
                .thenReturn(Optional.empty());
        when(saldoRepository.inicializar("AF-002", PERIODO_ACTUAL)).thenReturn(saldoNuevo);
        when(saldoRepository.guardar(any())).thenReturn(saldoNuevo);
        when(aporteRepository.guardar(any())).thenReturn(aporteGuardado);

        useCase.registrar(command("AF-002", "500000", "key-5"));

        verify(saldoRepository).inicializar("AF-002", PERIODO_ACTUAL);
    }

    private RegistrarAporteCommand command(String afiliadoId, String monto, String key) {
        return new RegistrarAporteCommand(afiliadoId, new BigDecimal(monto), "APP_MOVIL", key);
    }

    private Aporte aporte(Long id, String afiliadoId, BigDecimal monto, boolean marcada) {
        return new Aporte(id, afiliadoId, monto, LocalDate.now(), "APP_MOVIL",
                PERIODO_ACTUAL, marcada, "key");
    }
}
