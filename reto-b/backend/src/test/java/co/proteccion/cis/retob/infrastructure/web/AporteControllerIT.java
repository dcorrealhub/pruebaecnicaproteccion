package co.proteccion.cis.retob.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:testit;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "aporte.tope-mensual=1000000",
        "aporte.umbral-revision=500000"
})
class AporteControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void postAporte_exitoso_retorna201() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "afiliadoId", "AF-TEST-001",
                "monto", "100000.00",
                "fecha", "2026-06-01",
                "canal", "WEB"
        ));

        mockMvc.perform(post("/api/aportes")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.afiliadoId").value("AF-TEST-001"))
                .andExpect(jsonPath("$.marcadaRevision").value(false));
    }

    @Test
    void postAporte_claveIdempotenteDuplicada_retornaMismoResultado() throws Exception {
        String clave = UUID.randomUUID().toString();
        String body = objectMapper.writeValueAsString(Map.of(
                "afiliadoId", "AF-TEST-002",
                "monto", "200000.00",
                "fecha", "2026-06-01",
                "canal", "APP_MOVIL"
        ));

        mockMvc.perform(post("/api/aportes")
                        .header("Idempotency-Key", clave)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/aportes")
                        .header("Idempotency-Key", clave)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.afiliadoId").value("AF-TEST-002"));
    }

    @Test
    void postAporte_sinHeaderIdempotencia_retorna400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "afiliadoId", "AF-TEST-003",
                "monto", "100000.00",
                "fecha", "2026-06-01",
                "canal", "WEB"
        ));

        mockMvc.perform(post("/api/aportes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("HEADER_REQUERIDO_AUSENTE"));
    }

    @Test
    void postAporte_montoExcedeTope_retorna422() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "afiliadoId", "AF-TEST-004",
                "monto", "1100000.00",
                "fecha", "2026-06-01",
                "canal", "SUCURSAL"
        ));

        mockMvc.perform(post("/api/aportes")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("TOPE_MENSUAL_EXCEDIDO"));
    }

    @Test
    void getConsolidado_sinAportes_retornaTotal0() throws Exception {
        mockMvc.perform(get("/api/aportes/AF-SIN-APORTES/consolidado")
                        .param("periodoDesde", "2026-01")
                        .param("periodoHasta", "2026-06"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAportado").value(0))
                .andExpect(jsonPath("$.detalle").isArray());
    }

    @Test
    void getConsolidado_rangoInvertido_retorna422() throws Exception {
        mockMvc.perform(get("/api/aportes/AF-TEST-005/consolidado")
                        .param("periodoDesde", "2026-06")
                        .param("periodoHasta", "2026-01"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("RANGO_PERIODO_INVALIDO"));
    }
}
