package co.proteccion.cis.retob.infrastructure.web;

import co.proteccion.cis.retob.domain.model.ConsolidadoAportes;
import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase;
import co.proteccion.cis.retob.support.AporteMother;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AporteController.class)
@ActiveProfiles("test")
class AporteControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    RegistrarAporteUseCase registrarUseCase;

    @MockitoBean
    ConsultarAportesUseCase consultarUseCase;

    private static final String URL_APORTES = "/api/aportes";
    private static final String BODY_VALIDO = """
            {"afiliadoId":"AF-001","monto":100000,"canal":"WEB","idempotenciaKey":"idem-001"}
            """;

    @Test
    void POST_requestValido_retorna201ConCamposCorrectos() throws Exception {
        given(registrarUseCase.registrar(any())).willReturn(AporteMother.normal());

        mockMvc.perform(post(URL_APORTES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.afiliadoId").value("AF-001"))
                .andExpect(jsonPath("$.marcadaRevision").value(false));
    }

    @Test
    void POST_afiliadoIdEnBlanco_retorna400ConErrorDeCampo() throws Exception {
        String body = """
                {"afiliadoId":"","monto":100000,"canal":"WEB","idempotenciaKey":"idem-001"}
                """;

        mockMvc.perform(post(URL_APORTES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.afiliadoId").exists());
    }

    @Test
    void POST_sinMonto_retorna400() throws Exception {
        String body = """
                {"afiliadoId":"AF-001","canal":"WEB","idempotenciaKey":"idem-001"}
                """;

        mockMvc.perform(post(URL_APORTES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_montoMenorAlMinimo_retorna400() throws Exception {
        String body = """
                {"afiliadoId":"AF-001","monto":0.00,"canal":"WEB","idempotenciaKey":"idem-001"}
                """;

        mockMvc.perform(post(URL_APORTES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.monto").exists());
    }

    @Test
    void POST_idempotenciaKeyEnBlanco_retorna400() throws Exception {
        String body = """
                {"afiliadoId":"AF-001","monto":100000,"canal":"WEB","idempotenciaKey":""}
                """;

        mockMvc.perform(post(URL_APORTES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.idempotenciaKey").exists());
    }

    @Test
    void POST_useCaseLanzaIllegalArgument_retorna422() throws Exception {
        given(registrarUseCase.registrar(any()))
                .willThrow(new IllegalArgumentException("El monto debe ser mayor a cero"));

        mockMvc.perform(post(URL_APORTES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("El monto debe ser mayor a cero"));
    }

    @Test
    void POST_useCaseLanzaOptimisticLockException_retorna409() throws Exception {
        given(registrarUseCase.registrar(any()))
                .willThrow(new OptimisticLockException("conflict"));

        mockMvc.perform(post(URL_APORTES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflicto de concurrencia, reintente la operacion"));
    }

    @Test
    void POST_useCaseLanzaRuntimeException_retorna500() throws Exception {
        given(registrarUseCase.registrar(any()))
                .willThrow(new RuntimeException("boom"));

        mockMvc.perform(post(URL_APORTES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Error interno del servidor"));
    }

    @Test
    void GET_consolidado_parametrosValidos_retorna200() throws Exception {
        ConsolidadoAportes consolidado = new ConsolidadoAportes(
                "AF-001", "2026-01", "2026-06",
                new BigDecimal("600000.00"),
                List.of(AporteMother.normal(), AporteMother.normal())
        );
        given(consultarUseCase.consultar(any())).willReturn(consolidado);

        mockMvc.perform(get(URL_APORTES + "/consolidado")
                        .param("afiliadoId", "AF-001")
                        .param("periodoDesde", "2026-01")
                        .param("periodoHasta", "2026-06"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.afiliadoId").value("AF-001"))
                .andExpect(jsonPath("$.totalAportado").value(600000.00))
                .andExpect(jsonPath("$.detalle.length()").value(2));
    }

    @Test
    void GET_consolidado_sinAfiliadoId_retorna4xx() throws Exception {
        mockMvc.perform(get(URL_APORTES + "/consolidado"))
                .andExpect(status().is4xxClientError());
    }
}
