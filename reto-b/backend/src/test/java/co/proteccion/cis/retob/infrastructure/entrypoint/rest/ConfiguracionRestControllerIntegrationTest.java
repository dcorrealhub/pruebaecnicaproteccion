package co.proteccion.cis.retob.infrastructure.entrypoint.rest;

import co.proteccion.cis.retob.infrastructure.drivenadapter.postgresql.entity.ParametroAporteEntity;
import co.proteccion.cis.retob.infrastructure.drivenadapter.postgresql.repository.ParametroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class ConfiguracionRestControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ParametroRepository parametroRepo;

    @BeforeEach
    void sembrar() {
        parametroRepo.deleteAll();
        ParametroAporteEntity global = new ParametroAporteEntity();
        global.setAfiliadoId(null);
        global.setTopeMensual(new BigDecimal("10000000"));
        global.setUmbralRevision(new BigDecimal("5000000"));
        parametroRepo.save(global);
    }

    @Test
    void obtener_devuelveLosGlobales() throws Exception {
        mockMvc.perform(get("/api/configuracion/parametros"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topeMensual").value(10000000))
                .andExpect(jsonPath("$.umbralRevision").value(5000000));
    }

    @Test
    void actualizar_persisteYSeReflejaEnConsultaPosterior() throws Exception {
        mockMvc.perform(put("/api/configuracion/parametros").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                 {"topeMensual": 20000000, "umbralRevision": 8000000}
                                 """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topeMensual").value(20000000));

        mockMvc.perform(get("/api/configuracion/parametros"))
                .andExpect(jsonPath("$.topeMensual").value(20000000));
    }

    @Test
    void actualizar_umbralMayorQueTope_devuelve400() throws Exception {
        mockMvc.perform(put("/api/configuracion/parametros").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                 {"topeMensual": 1000, "umbralRevision": 2000}
                                 """))
                .andExpect(status().isBadRequest());
    }
}
