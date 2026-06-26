package co.proteccion.cis.retob.domain.model;

import co.proteccion.cis.retob.domain.exception.TransicionEstadoInvalidaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static co.proteccion.cis.retob.domain.model.EstadoAporte.*;
import static org.assertj.core.api.Assertions.*;

@DisplayName("EstadoAporte — ciclo de vida y transiciones")
class EstadoAporteTest {

    @Nested
    @DisplayName("Transiciones válidas")
    class TransicionesValidas {

        @Test
        void pendiente_a_en_revision() {
            assertThat(PENDIENTE.transicionar(EN_REVISION)).isEqualTo(EN_REVISION);
        }

        @Test
        void pendiente_a_aprobado() {
            assertThat(PENDIENTE.transicionar(APROBADO)).isEqualTo(APROBADO);
        }

        @Test
        void pendiente_a_anulado() {
            assertThat(PENDIENTE.transicionar(ANULADO)).isEqualTo(ANULADO);
        }

        @Test
        void en_revision_a_aprobado() {
            assertThat(EN_REVISION.transicionar(APROBADO)).isEqualTo(APROBADO);
        }

        @Test
        void en_revision_a_rechazado() {
            assertThat(EN_REVISION.transicionar(RECHAZADO)).isEqualTo(RECHAZADO);
        }
    }

    @Nested
    @DisplayName("Transiciones inválidas — deben lanzar TransicionEstadoInvalidaException")
    class TransicionesInvalidas {

        @Test
        void pendiente_no_puede_ir_a_rechazado() {
            assertThatThrownBy(() -> PENDIENTE.transicionar(RECHAZADO))
                    .isInstanceOf(TransicionEstadoInvalidaException.class)
                    .hasMessageContaining("PENDIENTE")
                    .hasMessageContaining("RECHAZADO");
        }

        @Test
        void pendiente_no_puede_ir_a_si_mismo() {
            assertThatThrownBy(() -> PENDIENTE.transicionar(PENDIENTE))
                    .isInstanceOf(TransicionEstadoInvalidaException.class);
        }

        @Test
        void en_revision_no_puede_volver_a_pendiente() {
            assertThatThrownBy(() -> EN_REVISION.transicionar(PENDIENTE))
                    .isInstanceOf(TransicionEstadoInvalidaException.class)
                    .hasMessageContaining("EN_REVISION")
                    .hasMessageContaining("PENDIENTE");
        }

        @Test
        void en_revision_no_puede_anularse() {
            assertThatThrownBy(() -> EN_REVISION.transicionar(ANULADO))
                    .isInstanceOf(TransicionEstadoInvalidaException.class)
                    .hasMessageContaining("EN_REVISION")
                    .hasMessageContaining("ANULADO");
        }

        @ParameterizedTest(name = "APROBADO → {0} es inválido")
        @EnumSource(EstadoAporte.class)
        void aprobado_es_estado_terminal(EstadoAporte destino) {
            assertThatThrownBy(() -> APROBADO.transicionar(destino))
                    .isInstanceOf(TransicionEstadoInvalidaException.class);
        }

        @ParameterizedTest(name = "RECHAZADO → {0} es inválido")
        @EnumSource(EstadoAporte.class)
        void rechazado_es_estado_terminal(EstadoAporte destino) {
            assertThatThrownBy(() -> RECHAZADO.transicionar(destino))
                    .isInstanceOf(TransicionEstadoInvalidaException.class);
        }

        @ParameterizedTest(name = "ANULADO → {0} es inválido")
        @EnumSource(EstadoAporte.class)
        void anulado_es_estado_terminal(EstadoAporte destino) {
            assertThatThrownBy(() -> ANULADO.transicionar(destino))
                    .isInstanceOf(TransicionEstadoInvalidaException.class);
        }
    }
}
