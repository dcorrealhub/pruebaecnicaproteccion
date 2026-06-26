package co.proteccion.cis.retoa;

import co.proteccion.cis.retoa.dto.AporteRequest;
import co.proteccion.cis.retoa.domain.Aporte;
import co.proteccion.cis.retoa.service.AporteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AporteServiceTest {

    @Autowired
    private AporteService service;

    @Test
    void registrar_montoValido_retornaAporte() {
        var req = new AporteRequest("AF-001", new BigDecimal("500000"), "APP_MOVIL");

        Aporte result = service.registrar(req);

        assertNotNull(result.getId());
        assertEquals(new BigDecimal("500000"), result.getMonto());
        assertFalse(result.isMarcadaRevision());
    }

    @Test
    void registrar_montoSuperaUmbral_marcaRevision() {
        var req = new AporteRequest("AF-002", new BigDecimal("6000000"), "WEB");

        Aporte result = service.registrar(req);

        assertTrue(result.isMarcadaRevision());
    }

    @Test
    void registrar_montoNegativo_lanzaExcepcion() {
        var req = new AporteRequest("AF-001", new BigDecimal("-100"), "APP_MOVIL");

        assertThrows(IllegalArgumentException.class, () -> service.registrar(req));
    }

    @Test
    void registrar_afiliadoInexistente_creaSaldoYRegistra() {
        var req = new AporteRequest("AF-INEXISTENTE", new BigDecimal("100000"), "APP_MOVIL");

        Aporte result = service.registrar(req);

        assertNotNull(result.getId());
        assertEquals(new BigDecimal("100000"), result.getMonto());
    }
}
