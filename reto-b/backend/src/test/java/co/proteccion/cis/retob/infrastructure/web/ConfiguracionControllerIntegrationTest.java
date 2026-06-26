package co.proteccion.cis.retob.infrastructure.web;

import co.proteccion.cis.retob.infrastructure.persistence.entity.ParametroAporteEntity;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataParametroRepository;
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
class ConfiguracionControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired SpringDataParametroRepository parametroRepo;

    @BeforeEach
    void sembrar() {
        parametroRepo.deleteAll();
        parametroRepo.save(ParametroAporteEntity.builder()
                .afiliadoId(null)
                .topeMensual(new BigDecimal("10000000"))
                .umbralRevision(new BigDecimal("5000000"))
                .build());
    }

    @Test
    void obtener_devuelveLosParametrosGlobales() throws Exception {
        mockMvc.perform(get("/api/configuracion/parametros"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topeMensual").value(10000000))
                .andExpect(jsonPath("$.umbralRevision").value(5000000));
    }

    @Test
    void actualizar_persisteYSeReflejaEnLaConsultaPosterior() throws Exception {
        mockMvc.perform(put("/api/configuracion/parametros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                 {"topeMensual": 20000000, "umbralRevision": 8000000}
                                 """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topeMensual").value(20000000))
                .andExpect(jsonPath("$.umbralRevision").value(8000000));

        mockMvc.perform(get("/api/configuracion/parametros"))
                .andExpect(jsonPath("$.topeMensual").value(20000000));
    }

    @Test
    void actualizar_umbralMayorQueTope_devuelve400() throws Exception {
        mockMvc.perform(put("/api/configuracion/parametros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                 {"topeMensual": 1000, "umbralRevision": 2000}
                                 """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizar_valorNoPositivo_devuelve400() throws Exception {
        mockMvc.perform(put("/api/configuracion/parametros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                 {"topeMensual": 0, "umbralRevision": 0}
                                 """))
                .andExpect(status().isBadRequest());
    }
}
