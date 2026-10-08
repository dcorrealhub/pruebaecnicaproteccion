package co.proteccion.cis.retob.domain.service;

import co.proteccion.cis.retob.domain.exception.ReglaNegocioException;
import co.proteccion.cis.retob.domain.model.Canal;
import co.proteccion.cis.retob.domain.model.ParametrosAfiliado;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PoliticaAportesTest {

    private final PoliticaAportes politica =
            new PoliticaAportes(new BigDecimal("10000000"), new BigDecimal("5000000"),
                    Map.of(Canal.SUCURSAL, new BigDecimal("3000000")));

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "-1", "-0.01"})
    void rechazaMontoNoPositivo(String monto) {
        assertThatThrownBy(() -> politica.validarMonto(new BigDecimal(monto)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("mayor a cero");
    }

    @Test
    void rechazaMontoNulo() {
        assertThatThrownBy(() -> politica.validarMonto(null)).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void rechazaMasDeDosDecimales() {
        assertThatThrownBy(() -> politica.validarMonto(new BigDecimal("100.001")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("2 decimales");
    }

    @Test
    void aceptaCerosNoSignificativos() {
        assertThatCode(() -> politica.validarMonto(new BigDecimal("100.000"))).doesNotThrowAnyException();
    }

    @Test
    void permiteLlegarExactamenteAlTope() {
        assertThatCode(() -> politica.validarTopeMensual(
                new BigDecimal("9000000"), new BigDecimal("1000000"), new BigDecimal("10000000"), "2026-10"))
                .doesNotThrowAnyException();
    }

    @Test
    void rechazaSuperarElTopePorUnCentavo() {
        assertThatThrownBy(() -> politica.validarTopeMensual(
                new BigDecimal("9000000"), new BigDecimal("1000000.01"), new BigDecimal("10000000"), "2026-10"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("tope mensual")
                .hasMessageContaining("2026-10");
    }

    @Test
    void usaLosParametrosPropiosDelAfiliadoSiExisten() {
        var propios = new ParametrosAfiliado(new BigDecimal("2000000"), new BigDecimal("300000"));

        assertThat(politica.topeAplicable(propios)).isEqualByComparingTo("2000000");
        assertThat(politica.umbralAplicable(propios, Canal.WEB)).isEqualByComparingTo("300000");
    }

    @Test
    void usaLosValoresPorDefectoSiElAfiliadoNoTieneParametros() {
        assertThat(politica.topeAplicable(ParametrosAfiliado.POR_DEFECTO)).isEqualByComparingTo("10000000");
        assertThat(politica.umbralAplicable(ParametrosAfiliado.POR_DEFECTO, Canal.WEB)).isEqualByComparingTo("5000000");
    }

    @Test
    void cadaParametroCaeAlDefectoDeFormaIndependiente() {
        var soloUmbral = new ParametrosAfiliado(null, new BigDecimal("300000"));

        assertThat(politica.topeAplicable(soloUmbral)).isEqualByComparingTo("10000000");
        assertThat(politica.umbralAplicable(soloUmbral, Canal.APP_MOVIL)).isEqualByComparingTo("300000");
    }

    @Test
    void sucursalUsaSuPropioUmbral() {
        assertThat(politica.umbralAplicable(ParametrosAfiliado.POR_DEFECTO, Canal.SUCURSAL)).isEqualByComparingTo("3000000");
        assertThat(politica.umbralAplicable(ParametrosAfiliado.POR_DEFECTO, Canal.APP_MOVIL)).isEqualByComparingTo("5000000");
    }

    @Test
    void elUmbralDelCanalPrevaleceSobreElDelAfiliado() {
        var umbralAlto = new ParametrosAfiliado(null, new BigDecimal("10000000"));
        var umbralBajo = new ParametrosAfiliado(null, new BigDecimal("1000000"));

        assertThat(politica.umbralAplicable(umbralAlto, Canal.SUCURSAL)).isEqualByComparingTo("3000000");
        assertThat(politica.umbralAplicable(umbralBajo, Canal.SUCURSAL)).isEqualByComparingTo("3000000");
        // en los demás canales sí aplica el umbral del afiliado
        assertThat(politica.umbralAplicable(umbralAlto, Canal.WEB)).isEqualByComparingTo("10000000");
    }

    @Test
    void noAceptaUmbralDeCanalNoPositivo() {
        assertThatThrownBy(() -> new PoliticaAportes(BigDecimal.TEN, BigDecimal.ONE, Map.of(Canal.SUCURSAL, BigDecimal.ZERO)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void montoIgualAlUmbralNoSeMarca() {
        assertThat(politica.requiereRevision(new BigDecimal("5000000.00"), new BigDecimal("5000000"))).isFalse();
    }

    @Test
    void montoMayorAlUmbralSeMarca() {
        assertThat(politica.requiereRevision(new BigDecimal("5000000.01"), new BigDecimal("5000000"))).isTrue();
    }

    @Test
    void noAceptaParametrosNoPositivos() {
        assertThatThrownBy(() -> new PoliticaAportes(BigDecimal.ZERO, BigDecimal.ONE, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
