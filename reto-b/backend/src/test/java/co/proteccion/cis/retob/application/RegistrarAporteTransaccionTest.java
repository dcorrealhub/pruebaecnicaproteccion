package co.proteccion.cis.retob.application;

import co.proteccion.cis.retob.application.usecase.RegistrarAporteTransaccion;
import co.proteccion.cis.retob.domain.exception.TopeMensualExcedidoException;
import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.Canal;
import co.proteccion.cis.retob.domain.model.ParametrosAporte;
import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase.RegistrarAporteCommand;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.EventoAportePort;
import co.proteccion.cis.retob.domain.port.out.ParametrosAportePort;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrarAporteTransaccionTest {

    @Mock AporteRepositoryPort aporteRepository;
    @Mock SaldoRepositoryPort saldoRepository;
    @Mock ParametrosAportePort parametrosPort;
    @Mock EventoAportePort eventoPort;

    @InjectMocks RegistrarAporteTransaccion transaccion;

    private static final ParametrosAporte PARAMS = new ParametrosAporte(
            new BigDecimal("10000000"), new BigDecimal("5000000"),
            java.util.Map.of(Canal.SUCURSAL, new BigDecimal("3000000")));

    private RegistrarAporteCommand comando(BigDecimal monto) {
        return comando(monto, Canal.WEB);
    }

    private RegistrarAporteCommand comando(BigDecimal monto, Canal canal) {
        return new RegistrarAporteCommand(
                "AF-001", monto, LocalDate.of(2025, 3, 10), canal, "key-1");
    }

    @Test
    void idempotencia_devuelve_original_sin_duplicar() {
        Aporte original = Aporte.nuevo("AF-001", new BigDecimal("1000"),
                LocalDate.of(2025, 3, 10), Canal.WEB, false, "key-1");
        when(aporteRepository.findByIdempotenciaKey("key-1")).thenReturn(Optional.of(original));

        Aporte resultado = transaccion.ejecutar(comando(new BigDecimal("1000")));

        assertThat(resultado).isSameAs(original);
        verify(aporteRepository, never()).guardar(any());
        verify(saldoRepository, never()).guardar(any());
        verify(eventoPort, never()).registrarCreado(any());
    }

    @Test
    void rechaza_por_tope_excedido() {
        when(parametrosPort.resolver("AF-001", "2025-03")).thenReturn(PARAMS);
        when(aporteRepository.findByIdempotenciaKey("key-1")).thenReturn(Optional.empty());
        when(saldoRepository.findByAfiliadoIdAndMes("AF-001", "2025-03"))
                .thenReturn(Optional.of(new SaldoMensual(1L, "AF-001", "2025-03",
                        new BigDecimal("9999999.99"), 0)));

        // acumulado 9.999.999,99 + 0,02 > tope 10.000.000 → rechazo
        assertThatThrownBy(() -> transaccion.ejecutar(comando(new BigDecimal("0.02"))))
                .isInstanceOf(TopeMensualExcedidoException.class);

        verify(aporteRepository, never()).guardar(any());
    }

    @Test
    void marca_para_revision_cuando_supera_umbral() {
        when(parametrosPort.resolver("AF-001", "2025-03")).thenReturn(PARAMS);
        when(aporteRepository.findByIdempotenciaKey("key-1")).thenReturn(Optional.empty());
        when(saldoRepository.findByAfiliadoIdAndMes("AF-001", "2025-03"))
                .thenReturn(Optional.of(new SaldoMensual(1L, "AF-001", "2025-03",
                        BigDecimal.ZERO.setScale(2), 0)));
        when(aporteRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        transaccion.ejecutar(comando(new BigDecimal("6000000")));

        ArgumentCaptor<Aporte> captor = ArgumentCaptor.forClass(Aporte.class);
        verify(aporteRepository).guardar(captor.capture());
        assertThat(captor.getValue().isMarcadaRevision()).isTrue();
    }

    @Test
    void inicializa_saldo_si_no_existe_y_actualiza_total() {
        when(parametrosPort.resolver("AF-001", "2025-03")).thenReturn(PARAMS);
        when(aporteRepository.findByIdempotenciaKey("key-1")).thenReturn(Optional.empty());
        when(saldoRepository.findByAfiliadoIdAndMes("AF-001", "2025-03")).thenReturn(Optional.empty());
        when(saldoRepository.inicializar("AF-001", "2025-03"))
                .thenReturn(new SaldoMensual(5L, "AF-001", "2025-03", BigDecimal.ZERO.setScale(2), 0));
        when(aporteRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        transaccion.ejecutar(comando(new BigDecimal("1000")));

        ArgumentCaptor<SaldoMensual> captor = ArgumentCaptor.forClass(SaldoMensual.class);
        verify(saldoRepository).guardar(captor.capture());
        assertThat(captor.getValue().getTotal()).isEqualByComparingTo("1000.00");
        verify(eventoPort).registrarCreado(any());
    }

    @Test
    void sucursal_marca_para_revision_con_umbral_mas_bajo() {
        // 4.000.000 por SUCURSAL supera el umbral de sucursal (3M) aunque no el default (5M).
        when(parametrosPort.resolver("AF-001", "2025-03")).thenReturn(PARAMS);
        when(aporteRepository.findByIdempotenciaKey("key-1")).thenReturn(Optional.empty());
        when(saldoRepository.findByAfiliadoIdAndMes("AF-001", "2025-03"))
                .thenReturn(Optional.of(new SaldoMensual(1L, "AF-001", "2025-03",
                        BigDecimal.ZERO.setScale(2), 0)));
        when(aporteRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        transaccion.ejecutar(comando(new BigDecimal("4000000"), Canal.SUCURSAL));

        ArgumentCaptor<Aporte> captor = ArgumentCaptor.forClass(Aporte.class);
        verify(aporteRepository).guardar(captor.capture());
        assertThat(captor.getValue().isMarcadaRevision()).isTrue();
    }
}
