package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.exception.AporteNotFoundException;
import co.proteccion.cis.retob.domain.exception.TransicionEstadoInvalidaException;
import co.proteccion.cis.retob.domain.model.*;
import co.proteccion.cis.retob.domain.port.in.CambiarEstadoAporteUseCase.CambiarEstadoCommand;
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
@DisplayName("CambiarEstadoAporteUseCaseImpl")
class CambiarEstadoAporteUseCaseImplTest {

    static final String APORTE_UUID = "550e8400-e29b-41d4-a716-446655440001";
    static final String AFILIADO    = "AF-001";
    static final String PERIODO     = "2026-06";
    static final BigDecimal MONTO   = new BigDecimal("3000000");

    @Mock AporteRepositoryPort  aporteRepository;
    @Mock RevisionRepository    revisionRepository;
    @Mock SaldoRepositoryPort   saldoRepository;
    @InjectMocks CambiarEstadoAporteUseCaseImpl useCase;

    private Aporte aporte(EstadoAporte estado) {
        return new Aporte(APORTE_UUID, AFILIADO, MONTO,
                LocalDate.now(), CanalOrigen.APP_MOVIL, PERIODO, estado, "k-001");
    }

    private SaldoMensual saldo(BigDecimal total) {
        return new SaldoMensual(1L, AFILIADO, PERIODO, total, 0);
    }

    private CambiarEstadoCommand comando(EstadoAporte nuevo) {
        return new CambiarEstadoCommand(APORTE_UUID, nuevo, "OP-01", "Comentario");
    }

    @Nested @DisplayName("Aporte no encontrado")
    class NotFound {
        @Test @DisplayName("lanza AporteNotFoundException cuando el UUID no existe")
        void lanza_not_found() {
            when(aporteRepository.findById(APORTE_UUID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.cambiar(comando(EstadoAporte.APROBADO)))
                    .isInstanceOf(AporteNotFoundException.class)
                    .hasMessageContaining(APORTE_UUID);

            verifyNoInteractions(revisionRepository, saldoRepository);
        }
    }

    @Nested @DisplayName("Transición válida")
    class TransicionValida {
        @Test @DisplayName("EN_REVISION → APROBADO guarda aporte y revisión sin tocar saldo")
        void en_revision_a_aprobado() {
            when(aporteRepository.findById(APORTE_UUID)).thenReturn(Optional.of(aporte(EstadoAporte.EN_REVISION)));
            when(aporteRepository.guardar(any())).thenReturn(aporte(EstadoAporte.APROBADO));

            Aporte resultado = useCase.cambiar(new CambiarEstadoCommand(APORTE_UUID, EstadoAporte.APROBADO, "OP-01", "OK"));

            assertThat(resultado.getEstado()).isEqualTo(EstadoAporte.APROBADO);
            verify(aporteRepository).guardar(any(Aporte.class));
            verify(revisionRepository).guardar(any(RevisionAporte.class));
            verifyNoInteractions(saldoRepository);
        }

        @Test @DisplayName("EN_REVISION → RECHAZADO libera el saldo mensual")
        void en_revision_a_rechazado_libera_saldo() {
            BigDecimal saldoActual = new BigDecimal("5000000");
            when(aporteRepository.findById(APORTE_UUID)).thenReturn(Optional.of(aporte(EstadoAporte.EN_REVISION)));
            when(aporteRepository.guardar(any())).thenReturn(aporte(EstadoAporte.RECHAZADO));
            when(saldoRepository.findByAfiliadoIdAndMes(AFILIADO, PERIODO))
                    .thenReturn(Optional.of(saldo(saldoActual)));

            useCase.cambiar(new CambiarEstadoCommand(APORTE_UUID, EstadoAporte.RECHAZADO, "OP-02", "Sospechoso"));

            ArgumentCaptor<SaldoMensual> saldoCaptor = ArgumentCaptor.forClass(SaldoMensual.class);
            verify(saldoRepository).guardar(saldoCaptor.capture());
            assertThat(saldoCaptor.getValue().getTotal())
                    .isEqualByComparingTo(saldoActual.subtract(MONTO)); // 5.000.000 - 3.000.000 = 2.000.000
        }

        @Test @DisplayName("RECHAZADO: si no hay saldo registrado no falla (ifPresent)")
        void rechazado_sin_saldo_no_lanza_excepcion() {
            when(aporteRepository.findById(APORTE_UUID)).thenReturn(Optional.of(aporte(EstadoAporte.EN_REVISION)));
            when(aporteRepository.guardar(any())).thenReturn(aporte(EstadoAporte.RECHAZADO));
            when(saldoRepository.findByAfiliadoIdAndMes(AFILIADO, PERIODO)).thenReturn(Optional.empty());

            assertThatCode(() -> useCase.cambiar(comando(EstadoAporte.RECHAZADO))).doesNotThrowAnyException();
            verify(saldoRepository, never()).guardar(any());
        }

        @Test @DisplayName("la revisión guardada contiene UUID, revisor, decisión y timestamp")
        void revision_contiene_datos_correctos() {
            when(aporteRepository.findById(APORTE_UUID)).thenReturn(Optional.of(aporte(EstadoAporte.EN_REVISION)));
            when(aporteRepository.guardar(any())).thenReturn(aporte(EstadoAporte.RECHAZADO));
            when(saldoRepository.findByAfiliadoIdAndMes(any(), any())).thenReturn(Optional.empty());

            ArgumentCaptor<RevisionAporte> captor = ArgumentCaptor.forClass(RevisionAporte.class);
            useCase.cambiar(new CambiarEstadoCommand(APORTE_UUID, EstadoAporte.RECHAZADO, "OP-02", "Sospechoso"));

            verify(revisionRepository).guardar(captor.capture());
            RevisionAporte rev = captor.getValue();
            assertThat(rev.getAporteId()).isEqualTo(APORTE_UUID);
            assertThat(rev.getRevisor()).isEqualTo("OP-02");
            assertThat(rev.getDecision()).isEqualTo(EstadoAporte.RECHAZADO);
            assertThat(rev.getComentario()).isEqualTo("Sospechoso");
            assertThat(rev.getOcurridoEn()).isNotNull();
        }
    }

    @Nested @DisplayName("Transición inválida")
    class TransicionInvalida {
        @Test @DisplayName("APROBADO es terminal — no persiste nada")
        void aprobado_es_terminal() {
            when(aporteRepository.findById(APORTE_UUID)).thenReturn(Optional.of(aporte(EstadoAporte.APROBADO)));

            assertThatThrownBy(() -> useCase.cambiar(comando(EstadoAporte.EN_REVISION)))
                    .isInstanceOf(TransicionEstadoInvalidaException.class);

            verify(aporteRepository, never()).guardar(any());
            verifyNoInteractions(revisionRepository, saldoRepository);
        }

        @Test @DisplayName("RECHAZADO es terminal — no persiste nada")
        void rechazado_es_terminal() {
            when(aporteRepository.findById(APORTE_UUID)).thenReturn(Optional.of(aporte(EstadoAporte.RECHAZADO)));

            assertThatThrownBy(() -> useCase.cambiar(comando(EstadoAporte.APROBADO)))
                    .isInstanceOf(TransicionEstadoInvalidaException.class);

            verify(aporteRepository, never()).guardar(any());
            verifyNoInteractions(revisionRepository, saldoRepository);
        }
    }
}
