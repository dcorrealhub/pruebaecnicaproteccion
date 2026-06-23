package co.proteccion.cis.retob.domain.service;

import co.proteccion.cis.retob.domain.exception.CanalInvalidoException;
import co.proteccion.cis.retob.domain.exception.MontoInvalidoException;
import co.proteccion.cis.retob.domain.exception.TopeMensualExcedidoException;
import co.proteccion.cis.retob.domain.model.CanalAporte;
import co.proteccion.cis.retob.domain.model.EstadoAporte;
import co.proteccion.cis.retob.domain.model.SaldoMensual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AporteDomainServiceTest {

    private AporteDomainService service;

    @BeforeEach
    void setUp() {
        service = new AporteDomainService();
    }

    @Test
    void validarMontoPositivo_montoCero_lanzaExcepcion() {
        assertThrows(MontoInvalidoException.class, () -> service.validarMontoPositivo(BigDecimal.ZERO));
    }

    @Test
    void validarMontoPositivo_montoNegativo_lanzaExcepcion() {
        assertThrows(MontoInvalidoException.class, () -> service.validarMontoPositivo(new BigDecimal("-1")));
    }

    @Test
    void validarTopeMensual_excedeTope_lanzaExcepcion() {
        SaldoMensual saldo = new SaldoMensual(1L, "AF-001", "2026-06", new BigDecimal("9000000"), 0);

        assertThrows(TopeMensualExcedidoException.class,
                () -> service.validarTopeMensual(saldo, new BigDecimal("2000000"), new BigDecimal("10000000")));
    }

    @Test
    void validarTopeMensual_dentroDelTope_noLanzaExcepcion() {
        SaldoMensual saldo = new SaldoMensual(1L, "AF-001", "2026-06", new BigDecimal("9000000"), 0);

        service.validarTopeMensual(saldo, new BigDecimal("1000000"), new BigDecimal("10000000"));
    }

    @Test
    void determinarEstado_montoSuperaUmbral_requiereRevision() {
        EstadoAporte estado = service.determinarEstado(
                new BigDecimal("6000000"), new BigDecimal("5000000"));

        assertEquals(EstadoAporte.REQUIERE_REVISION, estado);
    }

    @Test
    void determinarEstado_montoIgualUmbral_registrado() {
        EstadoAporte estado = service.determinarEstado(
                new BigDecimal("5000000"), new BigDecimal("5000000"));

        assertEquals(EstadoAporte.REGISTRADO, estado);
    }

    @Test
    void parseCanal_canalValido_retornaEnum() {
        assertEquals(CanalAporte.WEB, service.parseCanal("web"));
    }

    @Test
    void parseCanal_canalInvalido_lanzaExcepcion() {
        assertThrows(CanalInvalidoException.class, () -> service.parseCanal("DESCONOCIDO"));
    }

    @Test
    void calcularPeriodo_formateaCorrectamente() {
        assertEquals("2026-06", service.calcularPeriodo(LocalDate.of(2026, 6, 15)));
    }
}
