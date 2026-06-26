package co.proteccion.cis.retob.infrastructure.web;

import co.proteccion.cis.retob.infrastructure.persistence.entity.ParametroAporteEntity;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataAporteRepository;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataEventoRepository;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataParametroRepository;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataSaldoRepository;
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
 * Prueba de integración del contrato HTTP completo (controller → caso de uso → JPA/H2).
 * Cubre: registro, idempotencia, validación, tope mensual, umbral de revisión y aprobación.
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
class AporteControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired SpringDataAporteRepository aporteRepo;
    @Autowired SpringDataSaldoRepository saldoRepo;
    @Autowired SpringDataEventoRepository eventoRepo;
    @Autowired SpringDataParametroRepository parametroRepo;

    @BeforeEach
    void limpiarYSembrar() {
        eventoRepo.deleteAll();
        aporteRepo.deleteAll();
        saldoRepo.deleteAll();
        parametroRepo.deleteAll();
        // Parámetros globales por defecto (lo que en producción inserta la migración V2)
        parametroRepo.save(ParametroAporteEntity.builder()
                .afiliadoId(null)
                .topeMensual(new BigDecimal("10000000"))
                .umbralRevision(new BigDecimal("5000000"))
                .build());
    }

    private String cuerpo(String afiliado, String monto, String canal, String key) {
        return """
               {"afiliadoId":"%s","monto":%s,"fecha":"2025-06-10","canal":"%s","idempotenciaKey":"%s"}
               """.formatted(afiliado, monto, canal, key);
    }

    private MvcResult registrar(String json) throws Exception {
        return mockMvc.perform(post("/api/aportes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andReturn();
    }

    @Test
    void registrar_aporteValido_devuelve201YAprobado() throws Exception {
        mockMvc.perform(post("/api/aportes")
                        .contentType(MediaType.APPLICATION_JSON)
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
        mockMvc.perform(post("/api/aportes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("AF-003", "0", "WEB", "k-zero")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.monto").exists());
    }

    @Test
    void registrar_acumuladoSuperaTope_devuelve422ConMensaje() throws Exception {
        // tope=10M, umbral=5M. Tres aportes de 4M (cada uno <= umbral): el tercero supera el tope.
        registrar(cuerpo("AF-004", "4000000", "WEB", "k-a"));
        registrar(cuerpo("AF-004", "4000000", "WEB", "k-b"));

        mockMvc.perform(post("/api/aportes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("AF-004", "4000000", "WEB", "k-c")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("REGLA_NEGOCIO"))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("tope mensual")));
    }

    @Test
    void registrar_superaUmbral_quedaPendiente_yLuegoSeAprueba() throws Exception {
        JsonNode pendiente = objectMapper.readTree(
                registrar(cuerpo("AF-005", "6000000", "APP_MOVIL", "k-rev")).getResponse().getContentAsString());
        assertThat(pendiente.get("estado").asText()).isEqualTo("PENDIENTE_REVISION");
        long id = pendiente.get("id").asLong();

        // Antes de aprobar, el consolidado no lo cuenta como aprobado.
        mockMvc.perform(get("/api/aportes/consolidado")
                        .param("afiliadoId", "AF-005")
                        .param("periodoDesde", "2025-06")
                        .param("periodoHasta", "2025-06"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAportado").value(0))
                .andExpect(jsonPath("$.totalEnRevision").value(6000000));

        // Aprobación: pasa a contar para el tope.
        mockMvc.perform(post("/api/aportes/{id}/aprobar", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADO"));

        mockMvc.perform(get("/api/aportes/consolidado")
                        .param("afiliadoId", "AF-005")
                        .param("periodoDesde", "2025-06")
                        .param("periodoHasta", "2025-06"))
                .andExpect(jsonPath("$.totalAportado").value(6000000))
                .andExpect(jsonPath("$.totalEnRevision").value(0));
    }

    @Test
    void pendienteReservaCupo_aporteposteriorQueExcedeElTopeSeRechaza() throws Exception {
        // 6M supera el umbral => PENDIENTE, pero reserva cupo del tope (10M).
        registrar(cuerpo("AF-007", "6000000", "WEB", "k-p1"));
        // 5M (<= umbral) => APROBADO, pero 6M(pendiente)+5M = 11M > 10M => rechazo.
        mockMvc.perform(post("/api/aportes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("AF-007", "5000000", "WEB", "k-p2")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("REGLA_NEGOCIO"));
    }

    @Test
    void rechazarPendiente_liberaCupoYPermiteNuevoAporte() throws Exception {
        JsonNode pendiente = objectMapper.readTree(
                registrar(cuerpo("AF-008", "6000000", "WEB", "k-r1")).getResponse().getContentAsString());
        long id = pendiente.get("id").asLong();

        // Rechazar libera los 6M reservados.
        mockMvc.perform(post("/api/aportes/{id}/rechazar", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADO"));

        // Ahora un aporte de 5M cabe sin problema (cupo liberado).
        mockMvc.perform(post("/api/aportes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("AF-008", "5000000", "WEB", "k-r2")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("APROBADO"));
    }

    @Test
    void consolidado_devuelveTotalYDetalle() throws Exception {
        registrar(cuerpo("AF-006", "1000000", "WEB", "k-1"));
        registrar(cuerpo("AF-006", "2000000", "SUCURSAL", "k-2"));

        mockMvc.perform(get("/api/aportes/consolidado")
                        .param("afiliadoId", "AF-006")
                        .param("periodoDesde", "2025-06")
                        .param("periodoHasta", "2025-06"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAportado").value(3000000))
                .andExpect(jsonPath("$.detalle.length()").value(2));
    }
}
