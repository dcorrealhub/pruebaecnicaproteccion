package co.proteccion.cis.retob.infrastructure.web;

import co.proteccion.cis.retob.infrastructure.persistence.entity.AporteEntity;
import co.proteccion.cis.retob.infrastructure.persistence.entity.SaldoMensualEntity;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataAporteRepository;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataSaldoRepository;
import co.proteccion.cis.retob.infrastructure.web.dto.AporteResponse;
import co.proteccion.cis.retob.infrastructure.web.dto.RegistrarAporteRequest;
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

    // ──────────────────────────────────────────────
    // 3.8 RED — POST /api/aportes con datos válidos
    // ──────────────────────────────────────────────

    @Test
    void registrar_cuandoDatosValidos_retorna201ConBody() {
        var request = new RegistrarAporteRequest("AF-001", new BigDecimal("150000"), "APP_MOVIL", "k-3-8");

        var response = rest.postForEntity("/api/aportes", request, AporteResponse.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        var body = response.getBody();
        assertNotNull(body);
        assertNotNull(body.id());
        assertEquals("AF-001", body.afiliadoId());
        assertEquals(0, new BigDecimal("150000").compareTo(body.monto()));
        assertEquals(LocalDate.now(), body.fecha());
        assertEquals("APP_MOVIL", body.canal());
        assertNotNull(body.periodo());
        assertFalse(body.marcadaRevision());
    }

    // ──────────────────────────────────────────────────────────────────
    // 3.9 RED — misma idempotenciaKey → 200 y mismo ID (idempotencia)
    // ──────────────────────────────────────────────────────────────────

    @Test
    void registrar_conMismaIdempotenciaKey_retorna200ConMismoId() {
        var request = new RegistrarAporteRequest("AF-002", new BigDecimal("250000"), "WEB", "idemp-3-9");

        // Primer llamado: debe crear y retornar 201
        var firstResponse = rest.postForEntity("/api/aportes", request, AporteResponse.class);
        assertEquals(HttpStatus.CREATED, firstResponse.getStatusCode());
        var firstBody = firstResponse.getBody();
        assertNotNull(firstBody);
        var primerId = firstBody.id();
        assertNotNull(primerId);

        // Segundo llamado con misma idempotenciaKey: debe retornar 200 con mismo ID
        var secondResponse = rest.postForEntity("/api/aportes", request, AporteResponse.class);
        assertEquals(HttpStatus.OK, secondResponse.getStatusCode());
        var secondBody = secondResponse.getBody();
        assertNotNull(secondBody);
        assertEquals(primerId, secondBody.id());
        assertEquals("AF-002", secondBody.afiliadoId());
        assertEquals(0, new BigDecimal("250000").compareTo(secondBody.monto()));
    }

    // ──────────────────────────────────────────────────────
    // Adicional — POST /api/aportes con datos inválidos
    // ──────────────────────────────────────────────────────

    @Test
    void registrar_cuandoAfiliadoIdVacio_retorna400() {
        var request = new RegistrarAporteRequest("", new BigDecimal("100000"), "APP_MOVIL", "k-invalid");

        var response = rest.postForEntity("/api/aportes", request, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
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