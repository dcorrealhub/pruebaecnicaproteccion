package co.proteccion.cis.retob.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AporteTest {

    private static final BigDecimal MONTO = new BigDecimal("100000");
    private static final LocalDate FECHA = LocalDate.of(2025, 6, 15);

    @Test
    void nuevo_derivaPeriodoDesdeLaFecha() {
        Aporte aporte = Aporte.nuevo("AF-001", MONTO, FECHA, "WEB", EstadoAporte.APROBADO, "k1");

        assertThat(aporte.getPeriodo()).isEqualTo("2025-06");
        assertThat(aporte.getId()).isNull();
        assertThat(aporte.isMarcadaRevision()).isFalse();
    }

    @Test
    void nuevo_rechazaMontoNoPositivo() {
        assertThatThrownBy(() ->
                Aporte.nuevo("AF-001", BigDecimal.ZERO, FECHA, "WEB", EstadoAporte.APROBADO, "k1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positivo");

        assertThatThrownBy(() ->
                Aporte.nuevo("AF-001", new BigDecimal("-1"), FECHA, "WEB", EstadoAporte.APROBADO, "k1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nuevo_rechazaDatosObligatoriosVacios() {
        assertThatThrownBy(() ->
                Aporte.nuevo(" ", MONTO, FECHA, "WEB", EstadoAporte.APROBADO, "k1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->
                Aporte.nuevo("AF-001", MONTO, FECHA, "WEB", EstadoAporte.APROBADO, " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pendiente_marcadaRevisionEsVerdadero() {
        Aporte aporte = Aporte.nuevo("AF-001", MONTO, FECHA, "WEB", EstadoAporte.PENDIENTE_REVISION, "k1");
        assertThat(aporte.isMarcadaRevision()).isTrue();
    }

    @Test
    void aprobar_desdePendiente_pasaAAprobado() {
        Aporte pendiente = Aporte.nuevo("AF-001", MONTO, FECHA, "WEB", EstadoAporte.PENDIENTE_REVISION, "k1");

        Aporte aprobado = pendiente.aprobar();

        assertThat(aprobado.getEstado()).isEqualTo(EstadoAporte.APROBADO);
        assertThat(aprobado.isMarcadaRevision()).isFalse();
    }

    @Test
    void aprobar_desdeEstadoNoPendiente_lanzaReglaNegocio() {
        Aporte aprobado = Aporte.nuevo("AF-001", MONTO, FECHA, "WEB", EstadoAporte.APROBADO, "k1");

        assertThatThrownBy(aprobado::aprobar).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(aprobado::rechazar).isInstanceOf(ReglaNegocioException.class);
    }
}
