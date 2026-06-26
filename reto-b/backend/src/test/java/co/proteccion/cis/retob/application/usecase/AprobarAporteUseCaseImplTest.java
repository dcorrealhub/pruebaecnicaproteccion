package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.AporteNoEncontradoException;
import co.proteccion.cis.retob.domain.model.EstadoAporte;
import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.EventoAportePort;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AprobarAporteUseCaseImplTest {

    @Mock AporteRepositoryPort aporteRepository;
    @Mock SaldoRepositoryPort saldoRepository;
    @Mock EventoAportePort eventoPort;
    @Mock PlatformTransactionManager transactionManager;

    AprobarAporteUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new AprobarAporteUseCaseImpl(
                aporteRepository, saldoRepository, eventoPort, transactionManager);
        when(aporteRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(saldoRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Aporte pendiente() {
        return new Aporte(7L, "AF-001", new BigDecimal("6000000"), LocalDate.of(2025, 6, 10),
                "WEB", "2025-06", EstadoAporte.PENDIENTE_REVISION, "k-rev");
    }

    @Test
    void aprobar_pendiente_cambiaEstadoSinTocarSaldo() {
        when(aporteRepository.findById(7L)).thenReturn(Optional.of(pendiente()));

        Aporte resultado = useCase.aprobar(7L);

        assertThat(resultado.getEstado()).isEqualTo(EstadoAporte.APROBADO);
        // El cupo ya estaba reservado al registrarse: aprobar no modifica el saldo.
        verify(saldoRepository, never()).guardar(any());
        verify(eventoPort).registrar(any(), org.mockito.ArgumentMatchers.eq(EventoAportePort.Tipo.APORTE_APROBADO));
    }

    @Test
    void aprobar_aporteInexistente_lanzaNoEncontrado() {
        when(aporteRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.aprobar(99L)).isInstanceOf(AporteNoEncontradoException.class);
    }

    @Test
    void rechazar_pendiente_liberaLaReservaDelSaldo() {
        when(aporteRepository.findById(7L)).thenReturn(Optional.of(pendiente()));
        when(saldoRepository.findByAfiliadoIdAndMes("AF-001", "2025-06"))
                .thenReturn(Optional.of(new SaldoMensual(1L, "AF-001", "2025-06", new BigDecimal("9000000"), 0)));

        Aporte resultado = useCase.rechazar(7L);

        assertThat(resultado.getEstado()).isEqualTo(EstadoAporte.RECHAZADO);
        ArgumentCaptor<SaldoMensual> captor = ArgumentCaptor.forClass(SaldoMensual.class);
        verify(saldoRepository).guardar(captor.capture());
        // 9.000.000 - 6.000.000 = 3.000.000
        assertThat(captor.getValue().getTotal()).isEqualByComparingTo("3000000");
        verify(eventoPort).registrar(any(), org.mockito.ArgumentMatchers.eq(EventoAportePort.Tipo.APORTE_RECHAZADO));
    }
}
