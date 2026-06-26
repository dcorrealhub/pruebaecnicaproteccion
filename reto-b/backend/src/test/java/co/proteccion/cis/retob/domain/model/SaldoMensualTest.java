package co.proteccion.cis.retob.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SaldoMensualTest {

    private SaldoMensual saldo(String total) {
        return new SaldoMensual(1L, "AF-001", "2026-06", new BigDecimal(total), 0);
    }

    @Test
    void calcularNuevoTotal_saldoCeroMasAporte_retornaMonto() {
        SaldoMensual s = saldo("0");
        BigDecimal resultado = s.calcularNuevoTotal(new BigDecimal("100000"));
        assertThat(resultado).isEqualByComparingTo("100000");
    }

    @Test
    void calcularNuevoTotal_saldoAcumuladoMasAporte_sumaCorrectamente() {
        SaldoMensual s = saldo("4000000");
        BigDecimal resultado = s.calcularNuevoTotal(new BigDecimal("1000000"));
        assertThat(resultado).isEqualByComparingTo("5000000");
    }

    @Test
    void calcularNuevoTotal_noMutaElSaldoOriginal() {
        SaldoMensual s = saldo("4000000");
        s.calcularNuevoTotal(new BigDecimal("1000000"));
        assertThat(s.getTotal()).isEqualByComparingTo("4000000");
    }

    @Test
    void conTotal_retornaInstanciaDistinta() {
        SaldoMensual s = saldo("100000");
        SaldoMensual resultado = s.conTotal(new BigDecimal("200000"));
        assertThat(resultado).isNotSameAs(s);
    }

    @Test
    void conTotal_nuevoTotalEsCorrecto() {
        SaldoMensual s = saldo("100000");
        SaldoMensual resultado = s.conTotal(new BigDecimal("999999"));
        assertThat(resultado.getTotal()).isEqualByComparingTo("999999");
    }

    @Test
    void conTotal_conservaIdAfiliadoYMes() {
        SaldoMensual s = saldo("100000");
        SaldoMensual resultado = s.conTotal(new BigDecimal("200000"));
        assertThat(resultado.getId()).isEqualTo(s.getId());
        assertThat(resultado.getAfiliadoId()).isEqualTo(s.getAfiliadoId());
        assertThat(resultado.getMes()).isEqualTo(s.getMes());
        assertThat(resultado.getVersion()).isEqualTo(s.getVersion());
    }

    @Test
    void conTotal_elOriginalNoEsMutado() {
        SaldoMensual s = saldo("100000");
        s.conTotal(new BigDecimal("999999"));
        assertThat(s.getTotal()).isEqualByComparingTo("100000");
    }
}
