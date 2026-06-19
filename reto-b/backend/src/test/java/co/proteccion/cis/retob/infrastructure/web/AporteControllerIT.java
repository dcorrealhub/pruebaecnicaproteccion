package co.proteccion.cis.retob.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:testdb_it;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "aporte.tope-mensual=10000000",
        "aporte.umbral-revision=5000000"
})
class AporteControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registrarAporte_datosValidos_retorna201() throws Exception {
        String clave = UUID.randomUUID().toString();
        String body = """
                {
                  "afiliadoId": "AF-001",
                  "monto": 1000000,
                  "canal": "WEB",
                  "idempotenciaKey": "%s"
                }
                """.formatted(clave);

        mockMvc.perform(post("/api/aportes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.afiliadoId").value("AF-001"))
                .andExpect(jsonPath("$.marcadaRevision").value(false));
    }

    @Test
    void registrarAporte_mismaClaveIdempotencia_retornaMismoAporte() throws Exception {
        String clave = UUID.randomUUID().toString();
        String body = """
                {
                  "afiliadoId": "AF-002",
                  "monto": 500000,
                  "canal": "APP_MOVIL",
                  "idempotenciaKey": "%s"
                }
                """.formatted(clave);

        String respuesta1 = mockMvc.perform(post("/api/aportes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // Segundo intento con la misma clave: debe retornar el mismo id
        mockMvc.perform(post("/api/aportes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(content().json(respuesta1));
    }

    @Test
    void registrarAporte_superaTopeMensual_retorna422() throws Exception {
        String afiliado = "AF-TOPE-" + UUID.randomUUID();

        // Primer aporte: 9.500.000
        mockMvc.perform(post("/api/aportes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyAporte(afiliado, "9500000", UUID.randomUUID().toString())))
                .andExpect(status().isCreated());

        // Segundo aporte: 1.000.000 → total 10.500.000 > tope
        mockMvc.perform(post("/api/aportes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyAporte(afiliado, "1000000", UUID.randomUUID().toString())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void registrarAporte_montoSuperaUmbral_quedaMarcadoRevision() throws Exception {
        String clave = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/aportes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyAporte("AF-003", "6000000", clave)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.marcadaRevision").value(true));
    }

    @Test
    void registrarAporte_camposRequeridos_retorna400() throws Exception {
        mockMvc.perform(post("/api/aportes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void consultarConsolidado_afiliadoConAportes_retornaTotalYDetalle() throws Exception {
        String afiliado = "AF-CONS-" + UUID.randomUUID();

        mockMvc.perform(post("/api/aportes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyAporte(afiliado, "1000000", UUID.randomUUID().toString())))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/aportes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyAporte(afiliado, "2000000", UUID.randomUUID().toString())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/aportes/consolidado")
                        .param("afiliadoId", afiliado)
                        .param("periodoDesde", "2026-01")
                        .param("periodoHasta", "2026-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAportado").value(3000000))
                .andExpect(jsonPath("$.detalle.length()").value(2));
    }

    @Test
    void registrarAporte_montoCero_retorna400() throws Exception {
        mockMvc.perform(post("/api/aportes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyAporte("AF-004", "0", UUID.randomUUID().toString())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registrarAporte_canalInvalido_retorna400() throws Exception {
        String body = """
                {
                  "afiliadoId": "AF-005",
                  "monto": 100000,
                  "canal": "CANAL_INEXISTENTE",
                  "idempotenciaKey": "%s"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/aportes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.canal").exists());
    }

    @Test
    void consultarConsolidado_sinAportes_retornaTotalCeroYDetalleVacio() throws Exception {
        mockMvc.perform(get("/api/aportes/consolidado")
                        .param("afiliadoId", "AF-INEXISTENTE-" + UUID.randomUUID())
                        .param("periodoDesde", "2026-01")
                        .param("periodoHasta", "2026-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAportado").value(0))
                .andExpect(jsonPath("$.detalle").isEmpty());
    }

    @Test
    void consultarConsolidado_periodoFormatoInvalido_retorna400() throws Exception {
        mockMvc.perform(get("/api/aportes/consolidado")
                        .param("afiliadoId", "AF-001")
                        .param("periodoDesde", "enero-2026")
                        .param("periodoHasta", "2026-12"))
                .andExpect(status().isBadRequest());
    }

    private String bodyAporte(String afiliadoId, String monto, String clave) {
        return """
                {
                  "afiliadoId": "%s",
                  "monto": %s,
                  "canal": "WEB",
                  "idempotenciaKey": "%s"
                }
                """.formatted(afiliadoId, monto, clave);
    }
}
