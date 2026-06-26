package co.proteccion.cis.retob.infrastructure.web;

import co.proteccion.cis.retob.infrastructure.persistence.entity.AporteEntity;
import co.proteccion.cis.retob.infrastructure.persistence.entity.SaldoMensualEntity;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataAporteRepository;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataSaldoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class AporteControllerIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private SpringDataAporteRepository aporteRepo;

    @Autowired
    private SpringDataSaldoRepository saldoRepo;

    @BeforeEach
    void setUp() {
        aporteRepo.deleteAll();
        saldoRepo.deleteAll();
    }

    @Test
    void consolidado_cuandoExistenAportes_retorna200ConTotalYDetalle() {
        saldoRepo.save(crearSaldo("AF-001", "2026-06", BigDecimal.ZERO));
        aporteRepo.save(crearAporte("AF-001", new BigDecimal("100000"), "2026-06-01", "k1"));
        aporteRepo.save(crearAporte("AF-001", new BigDecimal("200000"), "2026-06-15", "k2"));

        var response = rest.getForEntity(
                "/api/aportes/consolidado?afiliadoId=AF-001&periodoDesde=2026-01&periodoHasta=2026-06",
                String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        var body = response.getBody();
        assertNotNull(body);
        assertTrue(body.contains("300000.00"));
        assertTrue(body.contains("\"detalle\""));
        assertTrue(body.contains("\"afiliadoId\":\"AF-001\""));
    }

    @Test
    void consolidado_cuandoNoHayAportes_retorna200ConTotalCero() {
        saldoRepo.save(crearSaldo("AF-999", "2026-06", BigDecimal.ZERO));

        var response = rest.getForEntity(
                "/api/aportes/consolidado?afiliadoId=AF-999&periodoDesde=2026-01&periodoHasta=2026-06",
                String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        var body = response.getBody();
        assertNotNull(body);
        assertTrue(body.contains("\"totalAportado\":0"));
        assertTrue(body.contains("\"detalle\":[]"));
    }

    @Test
    void consolidado_conPeriodosInvertidos_retorna200() {
        saldoRepo.save(crearSaldo("AF-001", "2026-06", BigDecimal.ZERO));
        aporteRepo.save(crearAporte("AF-001", new BigDecimal("500000"), "2026-06-01", "k3"));

        var response = rest.getForEntity(
                "/api/aportes/consolidado?afiliadoId=AF-001&periodoDesde=2026-06&periodoHasta=2026-01",
                String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    private AporteEntity crearAporte(String afiliadoId, BigDecimal monto, String fecha, String key) {
        var e = new AporteEntity();
        e.setAfiliadoId(afiliadoId);
        e.setMonto(monto);
        e.setFecha(LocalDate.parse(fecha));
        e.setCanal("APP_MOVIL");
        e.setPeriodo("2026-06");
        e.setMarcadaRevision(false);
        e.setIdempotenciaKey(key);
        return e;
    }

    private SaldoMensualEntity crearSaldo(String afiliadoId, String mes, BigDecimal total) {
        var e = new SaldoMensualEntity();
        e.setAfiliadoId(afiliadoId);
        e.setMes(mes);
        e.setTotal(total);
        return e;
    }
}