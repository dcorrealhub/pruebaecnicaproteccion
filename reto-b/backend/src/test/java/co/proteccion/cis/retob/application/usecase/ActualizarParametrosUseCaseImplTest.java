package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.model.ParametrosFondo;
import co.proteccion.cis.retob.domain.port.in.ActualizarParametrosUseCase.ActualizarParametrosCommand;
import co.proteccion.cis.retob.domain.port.out.ParametroRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActualizarParametrosUseCaseImpl")
class ActualizarParametrosUseCaseImplTest {

    @Mock ParametroRepository parametroRepository;
    @InjectMocks ActualizarParametrosUseCaseImpl useCase;

    // ── Helpers ──────────────────────────────────────────────────────────────

    private ParametrosFondo parametrosGuardados(BigDecimal tope, BigDecimal umbral) {
        return new ParametrosFondo(1L, tope, umbral, "ADMIN", OffsetDateTime.now(), "test");
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Regla de negocio: umbral debe ser menor al tope")
    class ValidacionNegocio {

        @Test
        @DisplayName("umbralRevision igual al topeMensual lanza IllegalArgumentException")
        void umbral_igual_a_tope() {
            var command = new ActualizarParametrosCommand(
                    new BigDecimal("5000000"), new BigDecimal("5000000"), "ADMIN", "test");

            assertThatThrownBy(() -> useCase.actualizar(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("umbral")
                    .hasMessageContaining("tope");

            verifyNoInteractions(parametroRepository);
        }

        @Test
        @DisplayName("umbralRevision mayor al topeMensual lanza IllegalArgumentException")
        void umbral_mayor_que_tope() {
            var command = new ActualizarParametrosCommand(
                    new BigDecimal("5000000"), new BigDecimal("6000000"), "ADMIN", "test");

            assertThatThrownBy(() -> useCase.actualizar(command))
                    .isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(parametroRepository);
        }
    }

    @Nested
    @DisplayName("Caso válido")
    class CasoValido {

        @Test
        @DisplayName("umbralRevision menor al topeMensual persiste y retorna el nuevo registro")
        void persiste_correctamente() {
            BigDecimal tope   = new BigDecimal("10000000");
            BigDecimal umbral = new BigDecimal("4000000");
            var command = new ActualizarParametrosCommand(tope, umbral, "ADMIN", "Ajuste Q3");

            when(parametroRepository.guardarCambio(any()))
                    .thenReturn(parametrosGuardados(tope, umbral));

            ParametrosFondo resultado = useCase.actualizar(command);

            assertThat(resultado.getTopeMensual()).isEqualByComparingTo(tope);
            assertThat(resultado.getUmbralRevision()).isEqualByComparingTo(umbral);
        }

        @Test
        @DisplayName("el registro guardado lleva modificadoPor y comentario correctos")
        void registro_con_datos_correctos() {
            var command = new ActualizarParametrosCommand(
                    new BigDecimal("10000000"), new BigDecimal("3000000"), "compliance-bot", "Normativa 2026");

            ArgumentCaptor<ParametrosFondo> captor = ArgumentCaptor.forClass(ParametrosFondo.class);
            when(parametroRepository.guardarCambio(captor.capture()))
                    .thenReturn(parametrosGuardados(command.topeMensual(), command.umbralRevision()));

            useCase.actualizar(command);

            ParametrosFondo guardado = captor.getValue();
            assertThat(guardado.getModificadoPor()).isEqualTo("compliance-bot");
            assertThat(guardado.getComentario()).isEqualTo("Normativa 2026");
            assertThat(guardado.getModificadoEn()).isNotNull();
            assertThat(guardado.getId()).isNull(); // id lo asigna la DB
        }
    }
}
