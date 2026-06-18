package co.proteccion.cis.retoa;

import co.proteccion.cis.retoa.dto.AporteRequest;
import co.proteccion.cis.retoa.domain.Aporte;
import co.proteccion.cis.retoa.service.AporteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AporteServiceTest {

    @Autowired
    private AporteService service;

    @Test
    void registrar_montoValido_retornaAporte() {
        var req = new AporteRequest("AF-001", 500_000.0, "APP_MOVIL");

        Aporte result = service.registrar(req);

        assertNotNull(result.getId());
        assertEquals(500_000.0, result.getMonto());
        assertFalse(result.isMarcadaRevision());
    }

    @Test
    void registrar_montoSuperaUmbral_marcaRevision() {
        // AF-002 parte de saldo 0; 6M > umbral de revision (5M)
        var req = new AporteRequest("AF-002", 6_000_000.0, "WEB");

        Aporte result = service.registrar(req);

        assertTrue(result.isMarcadaRevision());
    }

    @Test
    void registrar_montoNegativo_lanzaExcepcion() {
        var req = new AporteRequest("AF-001", -100.0, "APP_MOVIL");

        assertThrows(IllegalArgumentException.class, () -> service.registrar(req));
    }

    @Test
    void registrar_afiliadoInexistente_lanzaExcepcion() {
        var req = new AporteRequest("AF-INEXISTENTE", 100_000.0, "APP_MOVIL");

        assertThrows(IllegalArgumentException.class, () -> service.registrar(req));
    }
}
