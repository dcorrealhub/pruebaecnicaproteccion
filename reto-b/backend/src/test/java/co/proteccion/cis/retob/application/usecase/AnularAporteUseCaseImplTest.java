package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.exception.AporteNoAutorizadoException;
import co.proteccion.cis.retob.domain.exception.AporteNotFoundException;
import co.proteccion.cis.retob.domain.exception.TransicionEstadoInvalidaException;
import co.proteccion.cis.retob.domain.model.*;
import co.proteccion.cis.retob.domain.port.in.AnularAporteUseCase.AnularAporteCommand;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.RevisionRepository;
import co.proteccion.cis.retob.domain.port.out.SaldoRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnularAporteUseCaseImpl")
class AnularAporteUseCaseImplTest {

    static final String APORTE_UUID  = "aporte-uuid-001";
    static final String AFILIADO     = "AF-001";
    static final String OTRO_AFILIADO = "AF-999";
    static final String PERIODO      = "2026-06";
    static final BigDecimal MONTO    = new BigDecimal("2000000");

    @Mock AporteRepositoryPort aporteRepository;
    @Mock RevisionRepository   revisionRepository;
    @Mock SaldoRepositoryPort  saldoRepository;
    @InjectMocks AnularAporteUseCaseImpl useCase;

    private Aporte aporte(EstadoAporte estado) {
        return new Aporte(APORTE_UUID, AFILIADO, MONTO,
                LocalDate.now(), CanalOrigen.WEB, PERIODO, estado, "key-001");
    }

    private SaldoMensual saldo(BigDecimal total) {
        return new SaldoMensual(1L, AFILIADO, PERIODO, total, 0);
    }

    private AnularAporteCommand comando(String afiliado) {
        return new AnularAporteCommand(APORTE_UUID, afiliado, "Ya no necesito el aporte");
    }

    @Nested @DisplayName("Aporte no encontrado")
    class NotFound {
        @Test @DisplayName("lanza AporteNotFoundException si el UUID no existe")
        void lanza_not_found() {
            when(aporteRepository.findById(APORTE_UUID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.anular(comando(AFILIADO)))
                    .isInstanceOf(AporteNotFoundException.class)
                    .hasMessageContaining(APORTE_UUID);

            verifyNoInteractions(revisionRepository, saldoRepository);
        }
    }

    @Nested @DisplayName("Validación de titularidad")
    class Titularidad {
        @Test @DisplayName("afiliado diferente al titular → AporteNoAutorizadoException")
        void afiliado_no_titular() {
            when(aporteRepository.findById(APORTE_UUID)).thenReturn(Optional.of(aporte(EstadoAporte.PENDIENTE)));

            assertThatThrownBy(() -> useCase.anular(comando(OTRO_AFILIADO)))
                    .isInstanceOf(AporteNoAutorizadoException.class)
                    .hasMessageContaining(APORTE_UUID)
                    .hasMessageContaining(OTRO_AFILIADO);

            verify(aporteRepository, never()).guardar(any());
            verifyNoInteractions(revisionRepository, saldoRepository);
        }
    }

    @Nested @DisplayName("Validación de estado")
    class ValidacionEstado {
        @Test @DisplayName("aporte EN_REVISION no puede anularse → TransicionEstadoInvalidaException")
        void en_revision_no_puede_anularse() {
            when(aporteRepository.findById(APORTE_UUID)).thenReturn(Optional.of(aporte(EstadoAporte.EN_REVISION)));

            assertThatThrownBy(() -> useCase.anular(comando(AFILIADO)))
                    .isInstanceOf(TransicionEstadoInvalidaException.class);

            verify(aporteRepository, never()).guardar(any());
        }

        @Test @DisplayName("aporte APROBADO no puede anularse → TransicionEstadoInvalidaException")
        void aprobado_no_puede_anularse() {
            when(aporteRepository.findById(APORTE_UUID)).thenReturn(Optional.of(aporte(EstadoAporte.APROBADO)));

            assertThatThrownBy(() -> useCase.anular(comando(AFILIADO)))
                    .isInstanceOf(TransicionEstadoInvalidaException.class);
        }
    }

    @Nested @DisplayName("Camino feliz")
    class CaminoFeliz {
        @Test @DisplayName("PENDIENTE → ANULADO persiste aporte en estado ANULADO")
        void anula_aporte_pendiente() {
            BigDecimal saldoActual = new BigDecimal("4000000");
            when(aporteRepository.findById(APORTE_UUID)).thenReturn(Optional.of(aporte(EstadoAporte.PENDIENTE)));
            when(aporteRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
            when(saldoRepository.findByAfiliadoIdAndMes(AFILIADO, PERIODO))
                    .thenReturn(Optional.of(saldo(saldoActual)));
            when(saldoRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

            Aporte resultado = useCase.anular(comando(AFILIADO));

            assertThat(resultado.getEstado()).isEqualTo(EstadoAporte.ANULADO);
        }

        @Test @DisplayName("el saldo mensual se reduce en el monto del aporte anulado")
        void saldo_decrementado_al_anular() {
            BigDecimal saldoActual = new BigDecimal("4000000");
            when(aporteRepository.findById(APORTE_UUID)).thenReturn(Optional.of(aporte(EstadoAporte.PENDIENTE)));
            when(aporteRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
            when(saldoRepository.findByAfiliadoIdAndMes(AFILIADO, PERIODO))
                    .thenReturn(Optional.of(saldo(saldoActual)));
            when(saldoRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

            useCase.anular(comando(AFILIADO));

            ArgumentCaptor<SaldoMensual> captor = ArgumentCaptor.forClass(SaldoMensual.class);
            verify(saldoRepository).guardar(captor.capture());
            assertThat(captor.getValue().getTotal())
                    .isEqualByComparingTo(saldoActual.subtract(MONTO)); // 4.000.000 - 2.000.000 = 2.000.000
        }

        @Test @DisplayName("la revisión queda registrada con el afiliado como responsable")
        void revision_registrada_con_afiliado() {
            when(aporteRepository.findById(APORTE_UUID)).thenReturn(Optional.of(aporte(EstadoAporte.PENDIENTE)));
            when(aporteRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
            when(saldoRepository.findByAfiliadoIdAndMes(any(), any())).thenReturn(Optional.empty());

            useCase.anular(comando(AFILIADO));

            ArgumentCaptor<RevisionAporte> captor = ArgumentCaptor.forClass(RevisionAporte.class);
            verify(revisionRepository).guardar(captor.capture());
            RevisionAporte rev = captor.getValue();
            assertThat(rev.getDecision()).isEqualTo(EstadoAporte.ANULADO);
            assertThat(rev.getRevisor()).isEqualTo(AFILIADO);
            assertThat(rev.getOcurridoEn()).isNotNull();
        }

        @Test @DisplayName("sin saldo registrado no lanza excepción (ifPresent)")
        void sin_saldo_no_falla() {
            when(aporteRepository.findById(APORTE_UUID)).thenReturn(Optional.of(aporte(EstadoAporte.PENDIENTE)));
            when(aporteRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
            when(saldoRepository.findByAfiliadoIdAndMes(any(), any())).thenReturn(Optional.empty());

            assertThatCode(() -> useCase.anular(comando(AFILIADO))).doesNotThrowAnyException();
            verify(saldoRepository, never()).guardar(any());
        }
    }
}
