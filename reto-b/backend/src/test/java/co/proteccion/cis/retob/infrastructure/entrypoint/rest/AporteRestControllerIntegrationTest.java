package co.proteccion.cis.retob.infrastructure.entrypoint.rest;

import co.proteccion.cis.retob.infrastructure.drivenadapter.postgresql.entity.ParametroAporteEntity;
import co.proteccion.cis.retob.infrastructure.drivenadapter.postgresql.repository.AporteRepository;
import co.proteccion.cis.retob.infrastructure.drivenadapter.postgresql.repository.EventoRepository;
import co.proteccion.cis.retob.infrastructure.drivenadapter.postgresql.repository.ParametroRepository;
import co.proteccion.cis.retob.infrastructure.drivenadapter.postgresql.repository.SaldoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integración del contrato HTTP completo (controller → handler → use case → JPA/H2).
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class AporteRestControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AporteRepository aporteRepo;
    @Autowired SaldoRepository saldoRepo;
    @Autowired EventoRepository eventoRepo;
    @Autowired ParametroRepository parametroRepo;

    @BeforeEach
    void limpiarYSembrar() {
        eventoRepo.deleteAll();
        aporteRepo.deleteAll();
        saldoRepo.deleteAll();
        parametroRepo.deleteAll();
        ParametroAporteEntity global = new ParametroAporteEntity();
        global.setAfiliadoId(null);
        global.setTopeMensual(new BigDecimal("10000000"));
        global.setUmbralRevision(new BigDecimal("5000000"));
        parametroRepo.save(global);
    }

    private String cuerpo(String afiliado, String monto, String canal, String key) {
        return """
               {"afiliadoId":"%s","monto":%s,"fecha":"2025-06-10","canal":"%s","idempotenciaKey":"%s"}
               """.formatted(afiliado, monto, canal, key);
    }

    private MvcResult registrar(String json) throws Exception {
        return mockMvc.perform(post("/api/aportes").contentType(MediaType.APPLICATION_JSON).content(json)).andReturn();
    }

    @Test
    void registrar_valido_devuelve201YAprobado() throws Exception {
        mockMvc.perform(post("/api/aportes").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("AF-001", "1000000", "WEB", "k-ok")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.estado").value("APROBADO"))
                .andExpect(jsonPath("$.marcadaRevision").value(false))
                .andExpect(jsonPath("$.periodo").value("2025-06"));
    }

    @Test
    void registrar_mismaClaveDosVeces_esIdempotente() throws Exception {
        String json = cuerpo("AF-002", "1000000", "WEB", "k-dup");
        JsonNode primero = objectMapper.readTree(registrar(json).getResponse().getContentAsString());
        JsonNode segundo = objectMapper.readTree(registrar(json).getResponse().getContentAsString());

        assertThat(primero.get("id").asLong()).isEqualTo(segundo.get("id").asLong());
        assertThat(aporteRepo.count()).isEqualTo(1);
    }

    @Test
    void registrar_montoCero_devuelve400() throws Exception {
        mockMvc.perform(post("/api/aportes").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("AF-003", "0", "WEB", "k-zero")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.monto").exists());
    }

    @Test
    void registrar_acumuladoSuperaTope_devuelve422() throws Exception {
        registrar(cuerpo("AF-004", "4000000", "WEB", "k-a"));
        registrar(cuerpo("AF-004", "4000000", "WEB", "k-b"));

        mockMvc.perform(post("/api/aportes").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("AF-004", "4000000", "WEB", "k-c")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("REGLA_NEGOCIO"))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("tope mensual")));
    }

    @Test
    void pendienteReservaCupo_bloqueaAporteposterior_yRechazoLoLibera() throws Exception {
        JsonNode pendiente = objectMapper.readTree(
                registrar(cuerpo("AF-005", "6000000", "WEB", "k-p1")).getResponse().getContentAsString());
        assertThat(pendiente.get("estado").asText()).isEqualTo("PENDIENTE_REVISION");
        long id = pendiente.get("id").asLong();

        // 6M pendiente reserva cupo: +5M excede el tope de 10M.
        mockMvc.perform(post("/api/aportes").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("AF-005", "5000000", "WEB", "k-p2")))
                .andExpect(status().isUnprocessableEntity());

        // Rechazar libera los 6M.
        mockMvc.perform(post("/api/aportes/{id}/rechazar", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADO"));

        mockMvc.perform(post("/api/aportes").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("AF-005", "5000000", "WEB", "k-p3")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("APROBADO"));
    }

    @Test
    void cicloPendienteAprobado_seReflejaEnConsolidado() throws Exception {
        JsonNode pendiente = objectMapper.readTree(
                registrar(cuerpo("AF-006", "6000000", "APP_MOVIL", "k-rev")).getResponse().getContentAsString());
        long id = pendiente.get("id").asLong();

        mockMvc.perform(get("/api/aportes/consolidado")
                        .param("afiliadoId", "AF-006").param("periodoDesde", "2025-06").param("periodoHasta", "2025-06"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAportado").value(0))
                .andExpect(jsonPath("$.totalEnRevision").value(6000000));

        mockMvc.perform(post("/api/aportes/{id}/aprobar", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADO"));

        mockMvc.perform(get("/api/aportes/consolidado")
                        .param("afiliadoId", "AF-006").param("periodoDesde", "2025-06").param("periodoHasta", "2025-06"))
                .andExpect(jsonPath("$.totalAportado").value(6000000))
                .andExpect(jsonPath("$.totalEnRevision").value(0))
                .andExpect(jsonPath("$.detalle.length()").value(1));
    }
}
