package co.proteccion.cis.retob.domain.usecase;

import co.proteccion.cis.retob.domain.exception.DomainBusinessRuleException;
import co.proteccion.cis.retob.domain.exception.DomainNotFoundException;
import co.proteccion.cis.retob.domain.exception.DomainValidationException;
import co.proteccion.cis.retob.domain.model.aporte.Aporte;
import co.proteccion.cis.retob.domain.model.aporte.EstadoAporte;
import co.proteccion.cis.retob.domain.model.aporte.SaldoMensual;
import co.proteccion.cis.retob.domain.model.aporte.gateway.AporteOutputPort;
import co.proteccion.cis.retob.domain.model.aporte.gateway.EventoOutputPort;
import co.proteccion.cis.retob.domain.model.aporte.gateway.SaldoOutputPort;
import co.proteccion.cis.retob.domain.model.parametro.ParametrosAporte;
import co.proteccion.cis.retob.domain.model.parametro.gateway.ParametroOutputPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AporteUseCaseTest {

    private static final BigDecimal TOPE = new BigDecimal("10000000");
    private static final BigDecimal UMBRAL = new BigDecimal("5000000");

    @Mock AporteOutputPort aporteOutputPort;
    @Mock SaldoOutputPort saldoOutputPort;
    @Mock EventoOutputPort eventoOutputPort;
    @Mock ParametroOutputPort parametroOutputPort;

    AporteUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AporteUseCase(aporteOutputPort, saldoOutputPort, eventoOutputPort, parametroOutputPort);
        when(parametroOutputPort.forAfiliado(anyString())).thenReturn(new ParametrosAporte(TOPE, UMBRAL));
        when(aporteOutputPort.findByIdempotenciaKey(anyString())).thenReturn(Optional.empty());
        when(aporteOutputPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(saldoOutputPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Aporte comando(String monto, String key) {
        return Aporte.builder()
                .afiliadoId("AF-001")
                .monto(new BigDecimal(monto))
                .fecha(LocalDate.of(2025, 6, 10))
                .canal("WEB")
                .idempotenciaKey(key)
                .build();
    }

    private SaldoMensual saldo(String total) {
        return SaldoMensual.builder().id(1L).afiliadoId("AF-001").mes("2025-06")
                .total(new BigDecimal(total)).version(0).build();
    }

    @Test
    void registrar_idempotente_devuelveExistenteSinReprocesar() {
        Aporte existente = comando("100", "k-dup").toBuilder().id(9L).estado(EstadoAporte.APROBADO).build();
        when(aporteOutputPort.findByIdempotenciaKey("k-dup")).thenReturn(Optional.of(existente));

        Aporte resultado = useCase.registrar(comando("999", "k-dup"));

        assertThat(resultado).isSameAs(existente);
        verify(aporteOutputPort, never()).save(any());
        verify(saldoOutputPort, never()).save(any());
    }

    @Test
    void registrar_montoNoPositivo_lanzaValidacion() {
        assertThatThrownBy(() -> useCase.registrar(comando("0", "k1")))
                .isInstanceOf(DomainValidationException.class);
        verify(aporteOutputPort, never()).save(any());
    }

    @Test
    void registrar_superaUmbral_quedaPendienteYReservaCupo() {
        when(saldoOutputPort.findByAfiliadoIdAndMes("AF-001", "2025-06")).thenReturn(Optional.of(saldo("0")));

        Aporte resultado = useCase.registrar(comando("6000000", "k1"));

        assertThat(resultado.getEstado()).isEqualTo(EstadoAporte.PENDIENTE_REVISION);
        ArgumentCaptor<SaldoMensual> captor = ArgumentCaptor.forClass(SaldoMensual.class);
        verify(saldoOutputPort).save(captor.capture());
        assertThat(captor.getValue().getTotal()).isEqualByComparingTo("6000000");
        verify(eventoOutputPort).registrar(any(), eq(EventoOutputPort.Tipo.APORTE_MARCADO_REVISION));
    }

    @Test
    void registrar_dentroDeTope_seApruebaEIncrementaSaldo() {
        when(saldoOutputPort.findByAfiliadoIdAndMes("AF-001", "2025-06")).thenReturn(Optional.of(saldo("0")));

        Aporte resultado = useCase.registrar(comando("4000000", "k1"));

        assertThat(resultado.getEstado()).isEqualTo(EstadoAporte.APROBADO);
        assertThat(resultado.getPeriodo()).isEqualTo("2025-06");
        ArgumentCaptor<SaldoMensual> captor = ArgumentCaptor.forClass(SaldoMensual.class);
        verify(saldoOutputPort).save(captor.capture());
        assertThat(captor.getValue().getTotal()).isEqualByComparingTo("4000000");
    }

    @Test
    void registrar_acumuladoSuperaTope_lanzaReglaNegocio() {
        when(saldoOutputPort.findByAfiliadoIdAndMes("AF-001", "2025-06")).thenReturn(Optional.of(saldo("8000000")));

        assertThatThrownBy(() -> useCase.registrar(comando("4000000", "k1")))
                .isInstanceOf(DomainBusinessRuleException.class)
                .hasMessageContaining("tope mensual");
        verify(aporteOutputPort, never()).save(any());
    }

    @Test
    void aprobar_pendiente_pasaAAprobadoSinTocarSaldo() {
        Aporte pendiente = Aporte.builder().id(7L).afiliadoId("AF-001").monto(new BigDecimal("6000000"))
                .fecha(LocalDate.of(2025, 6, 10)).canal("WEB").periodo("2025-06")
                .estado(EstadoAporte.PENDIENTE_REVISION).idempotenciaKey("k").build();
        when(aporteOutputPort.findById(7L)).thenReturn(Optional.of(pendiente));

        Aporte resultado = useCase.aprobar(7L);

        assertThat(resultado.getEstado()).isEqualTo(EstadoAporte.APROBADO);
        verify(saldoOutputPort, never()).save(any());
        verify(eventoOutputPort).registrar(any(), eq(EventoOutputPort.Tipo.APORTE_APROBADO));
    }

    @Test
    void aprobar_inexistente_lanzaNoEncontrado() {
        when(aporteOutputPort.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.aprobar(99L)).isInstanceOf(DomainNotFoundException.class);
    }

    @Test
    void aprobar_noPendiente_lanzaReglaNegocio() {
        Aporte aprobado = Aporte.builder().id(7L).afiliadoId("AF-001").monto(new BigDecimal("100"))
                .fecha(LocalDate.of(2025, 6, 10)).canal("WEB").periodo("2025-06")
                .estado(EstadoAporte.APROBADO).idempotenciaKey("k").build();
        when(aporteOutputPort.findById(7L)).thenReturn(Optional.of(aprobado));

        assertThatThrownBy(() -> useCase.aprobar(7L)).isInstanceOf(DomainBusinessRuleException.class);
    }

    @Test
    void rechazar_pendiente_liberaLaReserva() {
        Aporte pendiente = Aporte.builder().id(7L).afiliadoId("AF-001").monto(new BigDecimal("6000000"))
                .fecha(LocalDate.of(2025, 6, 10)).canal("WEB").periodo("2025-06")
                .estado(EstadoAporte.PENDIENTE_REVISION).idempotenciaKey("k").build();
        when(aporteOutputPort.findById(7L)).thenReturn(Optional.of(pendiente));
        when(saldoOutputPort.findByAfiliadoIdAndMes("AF-001", "2025-06")).thenReturn(Optional.of(saldo("9000000")));

        Aporte resultado = useCase.rechazar(7L);

        assertThat(resultado.getEstado()).isEqualTo(EstadoAporte.RECHAZADO);
        ArgumentCaptor<SaldoMensual> captor = ArgumentCaptor.forClass(SaldoMensual.class);
        verify(saldoOutputPort).save(captor.capture());
        assertThat(captor.getValue().getTotal()).isEqualByComparingTo("3000000");
    }
}
