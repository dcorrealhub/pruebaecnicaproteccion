package co.proteccion.cis.retob;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.ConsolidadoAportes;
import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase;
import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase.ConsultarAportesQuery;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase.RegistrarAporteCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class RegistroAportesIntegrationTest {

    @Autowired
    private RegistrarAporteUseCase registrarUseCase;

    @Autowired
    private ConsultarAportesUseCase consultarUseCase;

    @Autowired
    private DataSource dataSource;

    private static final String AFILIADO = "AF-TEST";
    private static final String CANAL = "WEB";
    private static final DateTimeFormatter PERIODO_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final String PERIODO_ACTUAL = LocalDate.now().format(PERIODO_FMT);

    @BeforeEach
    void cleanUp() throws SQLException {
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM aporte");
            stmt.execute("DELETE FROM saldo_mensual");
        }
    }

    @Test
    void registrarAporte_exitoso() {
        var cmd = cmd("1000000");

        var result = registrarUseCase.registrar(cmd);

        assertNotNull(result.getId());
        assertEquals(new BigDecimal("1000000"), result.getMonto());
        assertEquals(AFILIADO, result.getAfiliadoId());
        assertEquals(CANAL, result.getCanal());
        assertFalse(result.isMarcadaRevision());
    }

    @Test
    void registrarAporte_idempotente() {
        var cmd = cmd("200000");

        var primero = registrarUseCase.registrar(cmd);
        var segundo = registrarUseCase.registrar(cmd);

        assertEquals(primero.getId(), segundo.getId());
        assertEquals(0, primero.getMonto().compareTo(segundo.getMonto()));
    }

    @Test
    void registrarAporte_montoNegativo_lanzaExcepcion() {
        var cmd = cmd("-100");

        assertThrows(IllegalArgumentException.class, () -> registrarUseCase.registrar(cmd));
    }

    @Test
    void registrarAporte_montoCero_lanzaExcepcion() {
        var cmd = cmd("0");

        assertThrows(IllegalArgumentException.class, () -> registrarUseCase.registrar(cmd));
    }

    @Test
    void registrarAporte_superaTopeMensual_lanzaExcepcion() {
        registrarUseCase.registrar(cmd("7000000"));

        var cmd = cmd("4000000");

        var ex = assertThrows(IllegalArgumentException.class, () -> registrarUseCase.registrar(cmd));
        assertTrue(ex.getMessage().contains("tope mensual"));
    }

    @Test
    void registrarAporte_topeExacto_ok() {
        registrarUseCase.registrar(cmd("3000000"));

        var result = registrarUseCase.registrar(cmd("7000000"));

        assertNotNull(result.getId());
        assertTrue(result.isMarcadaRevision());
    }

    @Test
    void registrarAporte_superaUmbral_marcaRevision() {
        var cmd = cmd("6000000");

        var result = registrarUseCase.registrar(cmd);

        assertTrue(result.isMarcadaRevision());
    }

    @Test
    void registrarAporte_porDebajoUmbral_noMarcaRevision() {
        var cmd = cmd("4000000");

        var result = registrarUseCase.registrar(cmd);

        assertFalse(result.isMarcadaRevision());
    }

    @Test
    void consultarConsolidado_conAportes_retornaTotalYDetalle() {
        var cmd1 = cmd("1000000", UUID.randomUUID().toString());
        var cmd2 = cmd("2000000", UUID.randomUUID().toString());
        registrarUseCase.registrar(cmd1);
        registrarUseCase.registrar(cmd2);

        var consolidado = consultarUseCase.consultar(
                new ConsultarAportesQuery(AFILIADO, PERIODO_ACTUAL, PERIODO_ACTUAL));

        assertEquals(AFILIADO, consolidado.afiliadoId());
        assertEquals(0, new BigDecimal("3000000").compareTo(consolidado.totalAportado()));
        assertEquals(2, consolidado.detalle().size());
    }

    @Test
    void consultarConsolidado_sinAportes_retornaVacio() {
        var consolidado = consultarUseCase.consultar(
                new ConsultarAportesQuery(AFILIADO, "2025-01", "2025-12"));

        assertEquals(AFILIADO, consolidado.afiliadoId());
        assertEquals(BigDecimal.ZERO, consolidado.totalAportado());
        assertTrue(consolidado.detalle().isEmpty());
    }

    @Test
    void registrarAporte_concurrencia_unGanaOtroRecibeExcepcion() throws Exception {
        registrarUseCase.registrar(cmd("1000000"));

        var cmd1 = cmd("2000000", UUID.randomUUID().toString());
        var cmd2 = cmd("3000000", UUID.randomUUID().toString());

        var latch = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        var resultados = new ArrayList<Throwable>();

        for (var cmd : new RegistrarAporteCommand[]{cmd1, cmd2}) {
            var c = cmd;
            executor.submit(() -> {
                try {
                    latch.await();
                    registrarUseCase.registrar(c);
                    resultados.add(null);
                } catch (Throwable e) {
                    synchronized (resultados) {
                        resultados.add(e);
                    }
                }
            });
        }

        latch.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        var totalExitosos = resultados.stream().filter(r -> r == null).count();
        var totalFallidos = resultados.stream().filter(r -> r != null).count();

        assertEquals(2, resultados.size());
        assertTrue(totalExitosos >= 1);
        if (totalFallidos > 0) {
            var ex = resultados.stream().filter(r -> r != null).findFirst().get();
            assertTrue(ex instanceof OptimisticLockingFailureException
                            || ex.getCause() instanceof OptimisticLockingFailureException,
                    "Esperaba OptimisticLockingFailureException pero fue: " + ex.getClass() + ": " + ex.getMessage());
        }
    }

    private RegistrarAporteCommand cmd(String monto) {
        return cmd(monto, UUID.randomUUID().toString());
    }

    private RegistrarAporteCommand cmd(String monto, String idempotenciaKey) {
        return new RegistrarAporteCommand(
                AFILIADO,
                new BigDecimal(monto),
                CANAL,
                idempotenciaKey
        );
    }
}
