package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.ConsolidadoAportes;
import co.proteccion.cis.retob.domain.model.enums.Canal;
import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase;
import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase.ConsultarAportesQuery;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase.RegistrarAporteCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.jdbc.JdbcTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integración contra PostgreSQL real: caso de uso + adaptador JPA + consulta derivada.
 */
@SpringBootTest
@ActiveProfiles("test")
class ConsultarAportesIntegrationTest {

    @Autowired
    private RegistrarAporteUseCase registrarAporteUseCase;
    @Autowired
    private ConsultarAportesUseCase consultarAportesUseCase;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void limpiarTablas() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "evento_aporte", "aporte", "saldo_mensual");
    }

    @Test
    void consultar_conAportesDentroYFueraDeRango_retornaTotalYDetalleSoloDelPeriodo() {
        // Arrange
        registrar("AF-100", "2025-02-28", "1000");   // antes del rango
        registrar("AF-100", "2025-03-10", "100");
        registrar("AF-100", "2025-05-20", "200");
        registrar("AF-100", "2025-05-01", "50");
        registrar("AF-100", "2025-06-01", "2000");   // después del rango
        registrar("AF-200", "2025-04-01", "5000");   // otro afiliado
        var query = new ConsultarAportesQuery("AF-100", "2025-03", "2025-05");

        // Act
        ConsolidadoAportes consolidado = consultarAportesUseCase.consultar(query);

        // Assert
        assertThat(consolidado.totalAportado()).isEqualByComparingTo("350");
        assertThat(consolidado.detalle())
                .extracting(Aporte::getFecha)
                .containsExactly(
                        LocalDate.parse("2025-03-10"),
                        LocalDate.parse("2025-05-01"),
                        LocalDate.parse("2025-05-20"));
        assertThat(consolidado.detalle())
                .extracting(Aporte::getAfiliadoId)
                .containsOnly("AF-100");
    }

    private void registrar(String afiliadoId, String fecha, String monto) {
        registrarAporteUseCase.registrar(new RegistrarAporteCommand(
                afiliadoId, new BigDecimal(monto), LocalDate.parse(fecha), Canal.WEB, UUID.randomUUID().toString()));
    }
}
