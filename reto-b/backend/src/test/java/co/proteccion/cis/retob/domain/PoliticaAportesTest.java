package co.proteccion.cis.retob.domain;

import co.proteccion.cis.retob.domain.exception.MontoInvalidoException;
import co.proteccion.cis.retob.domain.exception.TopeMensualExcedidoException;
import co.proteccion.cis.retob.domain.model.Canal;
import co.proteccion.cis.retob.domain.model.ParametrosAporte;
import co.proteccion.cis.retob.domain.model.PoliticaAportes;
import co.proteccion.cis.retob.domain.model.ResultadoEvaluacion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PoliticaAportesTest {

    // Tope 10.000.000; umbral por defecto 5.000.000; SUCURSAL 3.000.000.
    private static final ParametrosAporte PARAMS = new ParametrosAporte(
            new BigDecimal("10000000"),
            new BigDecimal("5000000"),
            Map.of(Canal.SUCURSAL, new BigDecimal("3000000")));

    @Test
    void rechaza_monto_cero() {
        assertThatThrownBy(() ->
                PoliticaAportes.evaluar(BigDecimal.ZERO, Canal.WEB, BigDecimal.ZERO, PARAMS))
                .isInstanceOf(MontoInvalidoException.class);
    }

    @Test
    void rechaza_monto_negativo() {
        assertThatThrownBy(() ->
                PoliticaAportes.evaluar(new BigDecimal("-1"), Canal.WEB, BigDecimal.ZERO, PARAMS))
                .isInstanceOf(MontoInvalidoException.class);
    }

    @Test
    void rechaza_monto_nulo() {
        assertThatThrownBy(() ->
                PoliticaAportes.evaluar(null, Canal.WEB, BigDecimal.ZERO, PARAMS))
                .isInstanceOf(MontoInvalidoException.class);
    }

    @Test
    void permite_aporte_justo_en_el_tope() {
        // acumulado 9.000.000 + 1.000.000 = 10.000.000 (== tope) → permitido
        ResultadoEvaluacion r = PoliticaAportes.evaluar(
                new BigDecimal("1000000"), Canal.WEB, new BigDecimal("9000000"), PARAMS);
        assertThat(r.marcadaRevision()).isFalse();
    }

    @Test
    void rechaza_aporte_un_centavo_por_encima_del_tope() {
        // acumulado 9.999.999,99 + 0,02 = 10.000.000,01 (> tope) → rechazo
        assertThatThrownBy(() -> PoliticaAportes.evaluar(
                new BigDecimal("0.02"), Canal.WEB, new BigDecimal("9999999.99"), PARAMS))
                .isInstanceOf(TopeMensualExcedidoException.class);
    }

    @Test
    void marca_revision_cuando_monto_supera_umbral_default() {
        // canal no-SUCURSAL: monto 5.000.001 > 5.000.000 → marcado
        ResultadoEvaluacion r = PoliticaAportes.evaluar(
                new BigDecimal("5000001"), Canal.WEB, BigDecimal.ZERO, PARAMS);
        assertThat(r.marcadaRevision()).isTrue();
    }

    @Test
    void no_marca_revision_cuando_monto_igual_al_umbral_default() {
        // monto == umbral → NO se marca (umbral es exclusivo)
        ResultadoEvaluacion r = PoliticaAportes.evaluar(
                new BigDecimal("5000000"), Canal.WEB, BigDecimal.ZERO, PARAMS);
        assertThat(r.marcadaRevision()).isFalse();
    }

    // --- Ajuste de requisito: SUCURSAL tiene umbral 3.000.000 ---

    @Test
    void sucursal_marca_revision_cuando_supera_3_millones() {
        // SUCURSAL: 3.000.001 > 3.000.000 → marcado
        ResultadoEvaluacion r = PoliticaAportes.evaluar(
                new BigDecimal("3000001"), Canal.SUCURSAL, BigDecimal.ZERO, PARAMS);
        assertThat(r.marcadaRevision()).isTrue();
    }

    @Test
    void sucursal_no_marca_cuando_igual_a_3_millones() {
        // umbral exclusivo: 3.000.000 == umbral → NO marca
        ResultadoEvaluacion r = PoliticaAportes.evaluar(
                new BigDecimal("3000000"), Canal.SUCURSAL, BigDecimal.ZERO, PARAMS);
        assertThat(r.marcadaRevision()).isFalse();
    }

    @Test
    void mismo_monto_marca_en_sucursal_pero_no_en_otros_canales() {
        // 4.000.000: supera el umbral de SUCURSAL (3M) pero no el default (5M)
        BigDecimal monto = new BigDecimal("4000000");
        assertThat(PoliticaAportes.evaluar(monto, Canal.SUCURSAL, BigDecimal.ZERO, PARAMS).marcadaRevision())
                .isTrue();
        assertThat(PoliticaAportes.evaluar(monto, Canal.WEB, BigDecimal.ZERO, PARAMS).marcadaRevision())
                .isFalse();
        assertThat(PoliticaAportes.evaluar(monto, Canal.APP_MOVIL, BigDecimal.ZERO, PARAMS).marcadaRevision())
                .isFalse();
    }
}
