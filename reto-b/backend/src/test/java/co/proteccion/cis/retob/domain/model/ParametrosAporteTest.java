package co.proteccion.cis.retob.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParametrosAporteTest {

    private final ParametrosAporte params =
            new ParametrosAporte(new BigDecimal("10000000"), new BigDecimal("5000000"));

    @Test
    void superaUmbral_estrictamenteMayor() {
        assertThat(params.superaUmbral(new BigDecimal("5000001"))).isTrue();
        assertThat(params.superaUmbral(new BigDecimal("5000000"))).isFalse();
        assertThat(params.superaUmbral(new BigDecimal("4999999"))).isFalse();
    }

    @Test
    void superaTope_estrictamenteMayor() {
        assertThat(params.superaTope(new BigDecimal("10000001"))).isTrue();
        assertThat(params.superaTope(new BigDecimal("10000000"))).isFalse();
    }

    @Test
    void constructor_rechazaValoresNoPositivos() {
        assertThatThrownBy(() -> new ParametrosAporte(BigDecimal.ZERO, new BigDecimal("1")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ParametrosAporte(new BigDecimal("1"), new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_rechazaUmbralMayorQueTope() {
        assertThatThrownBy(() -> new ParametrosAporte(new BigDecimal("1000"), new BigDecimal("2000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("umbral");
    }
}
