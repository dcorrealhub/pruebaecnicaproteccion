package co.proteccion.cis.retob.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

// Verifica las invariantes del constructor de Aporte sin dependencia de ningún framework.
class AporteTest {

    // El dominio debe rechazar un monto nulo antes de que llegue a cualquier capa superior.
    @Test
    void constructor_montoNulo_lanzaExcepcion() {
        assertThatThrownBy(() -> aporte(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positivo");
    }

    // Un aporte de cero no tiene sentido financiero; el dominio lo impide en la raíz.
    @Test
    void constructor_montoCero_lanzaExcepcion() {
        assertThatThrownBy(() -> aporte(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positivo");
    }

    // Un monto negativo no puede representar un aporte; el dominio lo rechaza explícitamente.
    @Test
    void constructor_montoNegativo_lanzaExcepcion() {
        assertThatThrownBy(() -> aporte(new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positivo");
    }

    // El mínimo válido (0.01) debe construir la instancia sin lanzar excepción.
    @Test
    void constructor_montoPositivo_creaInstanciaCorrectamente() {
        assertThatNoException().isThrownBy(() -> aporte(new BigDecimal("0.01")));
    }

    private Aporte aporte(BigDecimal monto) {
        return new Aporte(null, "AF-001", monto, LocalDate.now(), "WEB", "2025-06", false, "KEY-1");
    }
}
