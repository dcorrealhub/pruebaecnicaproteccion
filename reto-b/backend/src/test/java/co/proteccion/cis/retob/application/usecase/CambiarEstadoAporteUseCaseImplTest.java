package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.exception.AporteNotFoundException;
import co.proteccion.cis.retob.domain.exception.TransicionEstadoInvalidaException;
import co.proteccion.cis.retob.domain.model.*;
import co.proteccion.cis.retob.domain.port.in.CambiarEstadoAporteUseCase.CambiarEstadoCommand;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.domain.port.out.RevisionRepository;
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

    @Mock AporteRepositoryPort  aporteRepository;
    @Mock RevisionRepository    revisionRepository;

    @InjectMocks
    CambiarEstadoAporteUseCaseImpl useCase;

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Aporte aporte(EstadoAporte estado) {
        return new Aporte(1L, "AF-001", new BigDecimal("1000000"),
                LocalDate.now(), CanalOrigen.APP_MOVIL, "2026-06", estado, "k-001");
    }

    private CambiarEstadoCommand comando(EstadoAporte nuevo) {
        return new CambiarEstadoCommand(1L, nuevo, "OP-01", "Comentario de prueba");
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Aporte no encontrado")
    class NotFound {

        @Test
        @DisplayName("lanza AporteNotFoundException cuando el id no existe")
        void lanza_not_found() {
            when(aporteRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.cambiar(comando(EstadoAporte.APROBADO)))
                    .isInstanceOf(AporteNotFoundException.class)
                    .hasMessageContaining("1");

            verifyNoInteractions(revisionRepository);
        }
    }

    @Nested
    @DisplayName("Transición válida")
    class TransicionValida {

        @Test
        @DisplayName("EN_REVISION → APROBADO: guarda aporte con nuevo estado y crea revisión")
        void en_revision_a_aprobado() {
            Aporte enRevision = aporte(EstadoAporte.EN_REVISION);
            Aporte aprobado   = aporte(EstadoAporte.APROBADO);

            when(aporteRepository.findById(1L)).thenReturn(Optional.of(enRevision));
            when(aporteRepository.guardar(argThat(a -> a.getEstado() == EstadoAporte.APROBADO)))
                    .thenReturn(aprobado);
            when(revisionRepository.guardar(any())).thenReturn(null);

            Aporte resultado = useCase.cambiar(new CambiarEstadoCommand(1L, EstadoAporte.APROBADO, "OP-01", "OK"));

            assertThat(resultado.getEstado()).isEqualTo(EstadoAporte.APROBADO);
            verify(aporteRepository).guardar(any(Aporte.class));
            verify(revisionRepository).guardar(any(RevisionAporte.class));
        }

        @Test
        @DisplayName("la revisión guardada contiene el revisor, la decisión y el comentario correctos")
        void revision_contiene_datos_correctos() {
            when(aporteRepository.findById(1L)).thenReturn(Optional.of(aporte(EstadoAporte.EN_REVISION)));
            when(aporteRepository.guardar(any())).thenReturn(aporte(EstadoAporte.RECHAZADO));
            when(revisionRepository.guardar(any())).thenReturn(null);

            ArgumentCaptor<RevisionAporte> captor = ArgumentCaptor.forClass(RevisionAporte.class);
            useCase.cambiar(new CambiarEstadoCommand(1L, EstadoAporte.RECHAZADO, "OP-02", "Monto sospechoso"));

            verify(revisionRepository).guardar(captor.capture());
            RevisionAporte revision = captor.getValue();
            assertThat(revision.getRevisor()).isEqualTo("OP-02");
            assertThat(revision.getDecision()).isEqualTo(EstadoAporte.RECHAZADO);
            assertThat(revision.getComentario()).isEqualTo("Monto sospechoso");
            assertThat(revision.getAporteId()).isEqualTo(1L);
            assertThat(revision.getOcurridoEn()).isNotNull();
        }

        @Test
        @DisplayName("PENDIENTE → EN_REVISION es válido")
        void pendiente_a_en_revision() {
            when(aporteRepository.findById(1L)).thenReturn(Optional.of(aporte(EstadoAporte.PENDIENTE)));
            when(aporteRepository.guardar(any())).thenReturn(aporte(EstadoAporte.EN_REVISION));
            when(revisionRepository.guardar(any())).thenReturn(null);

            assertThatCode(() -> useCase.cambiar(comando(EstadoAporte.EN_REVISION)))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Transición inválida")
    class TransicionInvalida {

        @Test
        @DisplayName("APROBADO → cualquier estado lanza TransicionEstadoInvalidaException y no persiste")
        void aprobado_es_terminal() {
            when(aporteRepository.findById(1L)).thenReturn(Optional.of(aporte(EstadoAporte.APROBADO)));

            assertThatThrownBy(() -> useCase.cambiar(comando(EstadoAporte.EN_REVISION)))
                    .isInstanceOf(TransicionEstadoInvalidaException.class);

            verify(aporteRepository, never()).guardar(any());
            verifyNoInteractions(revisionRepository);
        }

        @Test
        @DisplayName("RECHAZADO → cualquier estado lanza TransicionEstadoInvalidaException y no persiste")
        void rechazado_es_terminal() {
            when(aporteRepository.findById(1L)).thenReturn(Optional.of(aporte(EstadoAporte.RECHAZADO)));

            assertThatThrownBy(() -> useCase.cambiar(comando(EstadoAporte.APROBADO)))
                    .isInstanceOf(TransicionEstadoInvalidaException.class);

            verify(aporteRepository, never()).guardar(any());
            verifyNoInteractions(revisionRepository);
        }
    }
}
