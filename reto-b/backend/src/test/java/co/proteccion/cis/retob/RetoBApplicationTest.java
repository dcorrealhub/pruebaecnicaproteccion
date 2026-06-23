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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrarAporteUseCaseImplTest {

    @Mock AporteRepositoryPort aporteRepository;
    @Mock SaldoRepositoryPort  saldoRepository;

    @InjectMocks
    RegistrarAporteUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(useCase, "topeMensual",    new BigDecimal("10000000"));
        ReflectionTestUtils.setField(useCase, "umbralRevision", new BigDecimal("5000000"));
    }

    @Test
    void registra_aporte_correctamente() {
        var command = new RegistrarAporteCommand("AF-001", new BigDecimal("100000"), "WEB", "key-1");
        var saldo   = new SaldoMensual(1L, "AF-001", "2025-06", BigDecimal.ZERO, 0);
        var aporte  = new Aporte(1L, "AF-001", new BigDecimal("100000"),
                LocalDate.now(), "WEB", "2025-06", false, "key-1");

        when(aporteRepository.findByIdempotenciaKey("key-1")).thenReturn(Optional.empty());
        when(saldoRepository.findByAfiliadoIdAndMes(any(), any())).thenReturn(Optional.of(saldo));
        when(aporteRepository.guardar(any())).thenReturn(aporte);
        when(saldoRepository.guardar(any())).thenReturn(saldo);

        Aporte resultado = useCase.registrar(command);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.isMarcadaRevision()).isFalse();
    }

    @Test
    void retorna_aporte_existente_si_idempotenciaKey_duplicada() {
        var aporte  = new Aporte(1L, "AF-001", new BigDecimal("100000"),
                LocalDate.now(), "WEB", "2025-06", false, "key-1");
        var command = new RegistrarAporteCommand("AF-001", new BigDecimal("100000"), "WEB", "key-1");

        when(aporteRepository.findByIdempotenciaKey("key-1")).thenReturn(Optional.of(aporte));

        Aporte resultado = useCase.registrar(command);

        assertThat(resultado).isEqualTo(aporte);
        verify(aporteRepository, never()).guardar(any());
    }

    @Test
    void rechaza_monto_negativo() {
        var command = new RegistrarAporteCommand("AF-001", new BigDecimal("-1"), "WEB", "key-2");

        when(aporteRepository.findByIdempotenciaKey("key-2")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.registrar(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("monto debe ser positivo");
    }

    @Test
    void rechaza_aporte_que_supera_tope_mensual() {
        var command = new RegistrarAporteCommand("AF-001", new BigDecimal("9000000"), "WEB", "key-3");
        var saldo   = new SaldoMensual(1L, "AF-001", "2025-06", new BigDecimal("5000000"), 0);

        when(aporteRepository.findByIdempotenciaKey("key-3")).thenReturn(Optional.empty());
        when(saldoRepository.findByAfiliadoIdAndMes(any(), any())).thenReturn(Optional.of(saldo));

        assertThatThrownBy(() -> useCase.registrar(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tope mensual");
    }

    @Test
    void marca_para_revision_si_supera_umbral() {
        var command = new RegistrarAporteCommand("AF-001", new BigDecimal("6000000"), "WEB", "key-4");
        var saldo   = new SaldoMensual(1L, "AF-001", "2025-06", BigDecimal.ZERO, 0);
        var aporte  = new Aporte(1L, "AF-001", new BigDecimal("6000000"),
                LocalDate.now(), "WEB", "2025-06", true, "key-4");

        when(aporteRepository.findByIdempotenciaKey("key-4")).thenReturn(Optional.empty());
        when(saldoRepository.findByAfiliadoIdAndMes(any(), any())).thenReturn(Optional.of(saldo));
        when(aporteRepository.guardar(any())).thenReturn(aporte);
        when(saldoRepository.guardar(any())).thenReturn(saldo);

        Aporte resultado = useCase.registrar(command);

        assertThat(resultado.isMarcadaRevision()).isTrue();
    }
}