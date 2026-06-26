package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.exception.AfiliadoNotFoundException;
import co.proteccion.cis.retob.domain.model.Afiliado;
import co.proteccion.cis.retob.domain.model.EstadoAfiliado;
import co.proteccion.cis.retob.domain.port.in.CambiarEstadoAfiliadoUseCase.CambiarEstadoAfiliadoCommand;
import co.proteccion.cis.retob.domain.port.out.AfiliadoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CambiarEstadoAfiliadoUseCaseImpl")
class CambiarEstadoAfiliadoUseCaseImplTest {

    static final String AFILIADO_ID = "AF-001";

    @Mock AfiliadoRepository afiliadoRepository;
    @InjectMocks CambiarEstadoAfiliadoUseCaseImpl useCase;

    private Afiliado afiliadoCon(EstadoAfiliado estado) {
        return new Afiliado("uuid-001", AFILIADO_ID, "Juan", estado, OffsetDateTime.now());
    }

    @Nested @DisplayName("Afiliado no encontrado")
    class NotFound {
        @Test @DisplayName("lanza AfiliadoNotFoundException si el ID no existe")
        void lanza_not_found() {
            when(afiliadoRepository.findByAfiliadoId(AFILIADO_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.cambiar(new CambiarEstadoAfiliadoCommand(AFILIADO_ID, EstadoAfiliado.BLOQUEADO)))
                    .isInstanceOf(AfiliadoNotFoundException.class)
                    .hasMessageContaining(AFILIADO_ID);

            verify(afiliadoRepository, never()).guardar(any());
        }
    }

    @Nested @DisplayName("Validación de estado")
    class ValidacionEstado {
        @Test @DisplayName("bloquear un afiliado ya BLOQUEADO lanza IllegalArgumentException")
        void ya_bloqueado() {
            when(afiliadoRepository.findByAfiliadoId(AFILIADO_ID))
                    .thenReturn(Optional.of(afiliadoCon(EstadoAfiliado.BLOQUEADO)));

            assertThatThrownBy(() -> useCase.cambiar(new CambiarEstadoAfiliadoCommand(AFILIADO_ID, EstadoAfiliado.BLOQUEADO)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("BLOQUEADO");

            verify(afiliadoRepository, never()).guardar(any());
        }

        @Test @DisplayName("desbloquear un afiliado ya ACTIVO lanza IllegalArgumentException")
        void ya_activo() {
            when(afiliadoRepository.findByAfiliadoId(AFILIADO_ID))
                    .thenReturn(Optional.of(afiliadoCon(EstadoAfiliado.ACTIVO)));

            assertThatThrownBy(() -> useCase.cambiar(new CambiarEstadoAfiliadoCommand(AFILIADO_ID, EstadoAfiliado.ACTIVO)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ACTIVO");

            verify(afiliadoRepository, never()).guardar(any());
        }
    }

    @Nested @DisplayName("Camino feliz")
    class CaminoFeliz {
        @Test @DisplayName("ACTIVO → BLOQUEADO actualiza el estado correctamente")
        void activo_a_bloqueado() {
            when(afiliadoRepository.findByAfiliadoId(AFILIADO_ID))
                    .thenReturn(Optional.of(afiliadoCon(EstadoAfiliado.ACTIVO)));
            when(afiliadoRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

            Afiliado resultado = useCase.cambiar(new CambiarEstadoAfiliadoCommand(AFILIADO_ID, EstadoAfiliado.BLOQUEADO));

            assertThat(resultado.getEstado()).isEqualTo(EstadoAfiliado.BLOQUEADO);
            assertThat(resultado.getAfiliadoId()).isEqualTo(AFILIADO_ID);
        }

        @Test @DisplayName("BLOQUEADO → ACTIVO reactiva el afiliado correctamente")
        void bloqueado_a_activo() {
            when(afiliadoRepository.findByAfiliadoId(AFILIADO_ID))
                    .thenReturn(Optional.of(afiliadoCon(EstadoAfiliado.BLOQUEADO)));
            when(afiliadoRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

            Afiliado resultado = useCase.cambiar(new CambiarEstadoAfiliadoCommand(AFILIADO_ID, EstadoAfiliado.ACTIVO));

            assertThat(resultado.getEstado()).isEqualTo(EstadoAfiliado.ACTIVO);
        }

        @Test @DisplayName("preserva nombre, afiliadoId y creadoEn al cambiar estado")
        void preserva_datos_inmutables() {
            Afiliado original = new Afiliado("uuid-001", AFILIADO_ID, "María Pérez",
                    EstadoAfiliado.ACTIVO, OffsetDateTime.parse("2025-01-01T00:00:00Z"));
            when(afiliadoRepository.findByAfiliadoId(AFILIADO_ID)).thenReturn(Optional.of(original));

            ArgumentCaptor<Afiliado> captor = ArgumentCaptor.forClass(Afiliado.class);
            when(afiliadoRepository.guardar(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            useCase.cambiar(new CambiarEstadoAfiliadoCommand(AFILIADO_ID, EstadoAfiliado.BLOQUEADO));

            Afiliado guardado = captor.getValue();
            assertThat(guardado.getNombre()).isEqualTo("María Pérez");
            assertThat(guardado.getAfiliadoId()).isEqualTo(AFILIADO_ID);
            assertThat(guardado.getCreadoEn()).isEqualTo(original.getCreadoEn());
            assertThat(guardado.getEstado()).isEqualTo(EstadoAfiliado.BLOQUEADO);
        }
    }
}
