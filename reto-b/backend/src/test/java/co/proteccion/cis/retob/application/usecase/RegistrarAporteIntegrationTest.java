package co.proteccion.cis.retob.application.usecase;

import co.proteccion.cis.retob.domain.model.enums.Canal;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase.Creado;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase.RegistrarAporteCommand;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase.RegistroAporte;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase.Repetido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.jdbc.JdbcTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integración contra PostgreSQL real: idempotencia de extremo a extremo con la restricción única.
 */
@SpringBootTest
@ActiveProfiles("test")
class RegistrarAporteIntegrationTest {

    @Autowired
    private RegistrarAporteUseCase registrarAporteUseCase;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void limpiarTablas() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "evento_aporte", "aporte", "saldo_mensual");
    }

    @Test
    void registrar_mismaClaveDosVeces_persisteUnSoloAporteYUnSoloEvento() {
        // Arrange
        var command = new RegistrarAporteCommand(
                "AF-300", new BigDecimal("150000"), LocalDate.parse("2025-07-15"), Canal.APP_MOVIL, "clave-reintento");

        // Act
        RegistroAporte primero = registrarAporteUseCase.registrar(command);
        RegistroAporte reintento = registrarAporteUseCase.registrar(command);

        // Assert
        assertThat(primero).isInstanceOf(Creado.class);
        assertThat(reintento).isInstanceOf(Repetido.class);
        assertThat(reintento.aporte().getId()).isEqualTo(primero.aporte().getId());
        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "aporte")).isEqualTo(1);
        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "evento_aporte")).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT total FROM saldo_mensual WHERE afiliado_id = 'AF-300' AND mes = '2025-07'", BigDecimal.class))
                .isEqualByComparingTo("150000");
    }
}
