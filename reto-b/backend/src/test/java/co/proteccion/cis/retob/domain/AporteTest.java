package co.proteccion.cis.retob.domain;

import co.proteccion.cis.retob.domain.exception.MontoInvalidoException;
import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.Canal;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AporteTest {

    @Test
    void nuevo_rechaza_monto_no_positivo() {
        assertThatThrownBy(() -> Aporte.nuevo(
                "AF-001", BigDecimal.ZERO, LocalDate.of(2025, 3, 10),
                Canal.WEB, false, "key-1"))
                .isInstanceOf(MontoInvalidoException.class);
    }

    @Test
    void nuevo_normaliza_monto_a_escala_2() {
        Aporte a = Aporte.nuevo(
                "AF-001", new BigDecimal("1000"), LocalDate.of(2025, 3, 10),
                Canal.WEB, false, "key-1");
        assertThat(a.getMonto().scale()).isEqualTo(2);
        assertThat(a.getMonto()).isEqualByComparingTo("1000.00");
    }

    @Test
    void nuevo_deriva_periodo_de_la_fecha() {
        Aporte a = Aporte.nuevo(
                "AF-001", new BigDecimal("1000"), LocalDate.of(2025, 3, 10),
                Canal.WEB, false, "key-1");
        assertThat(a.getPeriodo()).isEqualTo("2025-03");
    }
}
