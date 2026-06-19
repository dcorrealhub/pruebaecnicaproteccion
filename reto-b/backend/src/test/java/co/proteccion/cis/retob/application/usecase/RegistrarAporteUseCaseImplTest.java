package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase.RegistrarAporteCommand;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrarAporteUseCaseImplTest {

    @Mock AporteRepositoryPort aporteRepository;
    @Mock SaldoRepositoryPort saldoRepository;

    @InjectMocks RegistrarAporteUseCaseImpl useCase;

    private static final String PERIODO_ACTUAL =
            LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(useCase, "topeMensual", new BigDecimal("10000000"));
        ReflectionTestUtils.setField(useCase, "umbralRevision", new BigDecimal("5000000"));
    }

    @Test
    void registrar_idempotente_retorna_aporte_existente_sin_persistir_de_nuevo() {
        Aporte existente = aporte("key-1", new BigDecimal("1000000"), false);
        when(aporteRepository.findByIdempotenciaKey("key-1")).thenReturn(Optional.of(existente));

        Aporte resultado = useCase.registrar(new RegistrarAporteCommand("AF-001", new BigDecimal("1000000"), "WEB", "key-1"));

        assertThat(resultado).isSameAs(existente);
        verifyNoInteractions(saldoRepository);
        verify(aporteRepository, never()).guardar(any());
    }

    @Test
    void registrar_monto_cero_lanza_excepcion() {
        when(aporteRepository.findByIdempotenciaKey(any())).thenReturn(Optional.empty());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> useCase.registrar(
                        new RegistrarAporteCommand("AF-001", BigDecimal.ZERO, "WEB", "key-2")))
                .withMessageContaining("mayor a cero");
    }

    @Test
    void registrar_monto_negativo_lanza_excepcion() {
        when(aporteRepository.findByIdempotenciaKey(any())).thenReturn(Optional.empty());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> useCase.registrar(
                        new RegistrarAporteCommand("AF-001", new BigDecimal("-100"), "WEB", "key-3")));
    }

    @Test
    void registrar_supera_tope_mensual_lanza_excepcion() {
        when(aporteRepository.findByIdempotenciaKey(any())).thenReturn(Optional.empty());
        SaldoMensual saldoActual = saldo(new BigDecimal("8000000"));
        when(saldoRepository.findByAfiliadoIdAndMes("AF-001", PERIODO_ACTUAL))
                .thenReturn(Optional.of(saldoActual));

        // 8M acumulado + 3M nuevo = 11M > tope de 10M
        assertThatIllegalArgumentException()
                .isThrownBy(() -> useCase.registrar(
                        new RegistrarAporteCommand("AF-001", new BigDecimal("3000000"), "WEB", "key-4")))
                .withMessageContaining("tope mensual");

        verify(aporteRepository, never()).guardar(any());
    }

    @Test
    void registrar_exactamente_en_tope_mensual_es_valido() {
        when(aporteRepository.findByIdempotenciaKey(any())).thenReturn(Optional.empty());
        SaldoMensual saldoActual = saldo(new BigDecimal("7000000"));
        when(saldoRepository.findByAfiliadoIdAndMes("AF-001", PERIODO_ACTUAL))
                .thenReturn(Optional.of(saldoActual));
        when(saldoRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        Aporte esperado = aporte("key-5", new BigDecimal("3000000"), false);
        when(aporteRepository.guardar(any())).thenReturn(esperado);

        // 7M + 3M = 10M == tope: debe aceptarse
        Aporte resultado = useCase.registrar(
                new RegistrarAporteCommand("AF-001", new BigDecimal("3000000"), "WEB", "key-5"));

        assertThat(resultado).isNotNull();
    }

    @Test
    void registrar_monto_sobre_umbral_marca_revision() {
        when(aporteRepository.findByIdempotenciaKey(any())).thenReturn(Optional.empty());
        when(saldoRepository.findByAfiliadoIdAndMes(any(), any())).thenReturn(Optional.empty());
        SaldoMensual saldoInicial = saldo(BigDecimal.ZERO);
        when(saldoRepository.inicializar(any(), any())).thenReturn(saldoInicial);
        when(saldoRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        Aporte aporteGuardado = aporte("key-6", new BigDecimal("6000000"), true);
        when(aporteRepository.guardar(any())).thenReturn(aporteGuardado);

        Aporte resultado = useCase.registrar(
                new RegistrarAporteCommand("AF-001", new BigDecimal("6000000"), "APP_MOVIL", "key-6"));

        assertThat(resultado.isMarcadaRevision()).isTrue();
    }

    @Test
    void registrar_monto_bajo_umbral_no_marca_revision() {
        when(aporteRepository.findByIdempotenciaKey(any())).thenReturn(Optional.empty());
        when(saldoRepository.findByAfiliadoIdAndMes(any(), any())).thenReturn(Optional.empty());
        SaldoMensual saldoInicial = saldo(BigDecimal.ZERO);
        when(saldoRepository.inicializar(any(), any())).thenReturn(saldoInicial);
        when(saldoRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        Aporte aporteGuardado = aporte("key-7", new BigDecimal("1000000"), false);
        when(aporteRepository.guardar(any())).thenReturn(aporteGuardado);

        Aporte resultado = useCase.registrar(
                new RegistrarAporteCommand("AF-001", new BigDecimal("1000000"), "SUCURSAL", "key-7"));

        assertThat(resultado.isMarcadaRevision()).isFalse();
    }

    @Test
    void registrar_sin_saldo_previo_inicializa_saldo_y_persiste() {
        when(aporteRepository.findByIdempotenciaKey(any())).thenReturn(Optional.empty());
        when(saldoRepository.findByAfiliadoIdAndMes("AF-001", PERIODO_ACTUAL))
                .thenReturn(Optional.empty());
        SaldoMensual saldoInicial = saldo(BigDecimal.ZERO);
        when(saldoRepository.inicializar("AF-001", PERIODO_ACTUAL)).thenReturn(saldoInicial);
        when(saldoRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        Aporte aporteGuardado = aporte("key-8", new BigDecimal("500000"), false);
        when(aporteRepository.guardar(any())).thenReturn(aporteGuardado);

        useCase.registrar(new RegistrarAporteCommand("AF-001", new BigDecimal("500000"), "WEB", "key-8"));

        verify(saldoRepository).inicializar("AF-001", PERIODO_ACTUAL);
        verify(saldoRepository).guardar(argThat(s -> s.getTotal().compareTo(new BigDecimal("500000")) == 0));
        verify(aporteRepository).guardar(any());
    }

    // --- helpers ---

    private Aporte aporte(String key, BigDecimal monto, boolean marcadaRevision) {
        return new Aporte(1L, "AF-001", monto, LocalDate.now(), "WEB",
                PERIODO_ACTUAL, marcadaRevision, key);
    }

    private SaldoMensual saldo(BigDecimal total) {
        return new SaldoMensual(1L, "AF-001", PERIODO_ACTUAL, total, 0);
    }
}
