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

    static final BigDecimal MINIMO  = new BigDecimal("10000");
    static final BigDecimal TOPE    = new BigDecimal("10000000");
    static final BigDecimal UMBRAL  = new BigDecimal("5000000");

    @Mock ParametroRepository parametroRepository;
    @InjectMocks ActualizarParametrosUseCaseImpl useCase;

    private ParametrosFondo parametrosGuardados(BigDecimal minimo, BigDecimal tope, BigDecimal umbral) {
        return new ParametrosFondo("param-uuid-001", minimo, tope, umbral,
                "ADMIN", OffsetDateTime.now(), "test");
    }

    private ActualizarParametrosCommand comando(BigDecimal minimo, BigDecimal tope, BigDecimal umbral) {
        return new ActualizarParametrosCommand(minimo, tope, umbral, "ADMIN", "test");
    }

    @Nested
    @DisplayName("Invariante: montoMinimo < umbralRevision < topeMensual")
    class ValidacionNegocio {

        @Test
        @DisplayName("umbralRevision igual al topeMensual lanza IllegalArgumentException")
        void umbral_igual_a_tope() {
            assertThatThrownBy(() -> useCase.actualizar(comando(MINIMO, new BigDecimal("5000000"), new BigDecimal("5000000"))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("umbral")
                    .hasMessageContaining("tope");

            verifyNoInteractions(parametroRepository);
        }

        @Test
        @DisplayName("umbralRevision mayor al topeMensual lanza IllegalArgumentException")
        void umbral_mayor_que_tope() {
            assertThatThrownBy(() -> useCase.actualizar(comando(MINIMO, new BigDecimal("5000000"), new BigDecimal("6000000"))))
                    .isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(parametroRepository);
        }

        @Test
        @DisplayName("montoMinimo igual al umbralRevision lanza IllegalArgumentException")
        void minimo_igual_a_umbral() {
            assertThatThrownBy(() -> useCase.actualizar(comando(UMBRAL, TOPE, UMBRAL)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("monto mínimo");

            verifyNoInteractions(parametroRepository);
        }

        @Test
        @DisplayName("montoMinimo mayor al umbralRevision lanza IllegalArgumentException")
        void minimo_mayor_que_umbral() {
            assertThatThrownBy(() -> useCase.actualizar(
                    comando(new BigDecimal("6000000"), TOPE, UMBRAL)))
                    .isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(parametroRepository);
        }
    }

    @Nested
    @DisplayName("Caso válido")
    class CasoValido {

        @Test
        @DisplayName("parámetros coherentes (minimo < umbral < tope) persisten correctamente")
        void persiste_correctamente() {
            when(parametroRepository.guardarCambio(any()))
                    .thenReturn(parametrosGuardados(MINIMO, TOPE, UMBRAL));

            ParametrosFondo resultado = useCase.actualizar(comando(MINIMO, TOPE, UMBRAL));

            assertThat(resultado.getMontoMinimo()).isEqualByComparingTo(MINIMO);
            assertThat(resultado.getTopeMensual()).isEqualByComparingTo(TOPE);
            assertThat(resultado.getUmbralRevision()).isEqualByComparingTo(UMBRAL);
        }

        @Test
        @DisplayName("el registro guardado lleva modificadoPor y comentario correctos")
        void registro_con_datos_correctos() {
            var command = new ActualizarParametrosCommand(
                    new BigDecimal("100000"), new BigDecimal("10000000"),
                    new BigDecimal("3000000"), "compliance-bot", "Normativa 2026");

            ArgumentCaptor<ParametrosFondo> captor = ArgumentCaptor.forClass(ParametrosFondo.class);
            when(parametroRepository.guardarCambio(captor.capture()))
                    .thenReturn(parametrosGuardados(command.montoMinimo(), command.topeMensual(), command.umbralRevision()));

            useCase.actualizar(command);

            ParametrosFondo guardado = captor.getValue();
            assertThat(guardado.getModificadoPor()).isEqualTo("compliance-bot");
            assertThat(guardado.getComentario()).isEqualTo("Normativa 2026");
            assertThat(guardado.getModificadoEn()).isNotNull();
            assertThat(guardado.getId()).isNull();
        }
    }
}
