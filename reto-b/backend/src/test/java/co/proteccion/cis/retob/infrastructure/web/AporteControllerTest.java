package co.proteccion.cis.retob.infrastructure.web;

import co.proteccion.cis.retob.domain.exception.ConflictoConcurrenciaException;
import co.proteccion.cis.retob.domain.exception.RecursoNoEncontradoException;
import co.proteccion.cis.retob.domain.exception.TopeMensualExcedidoException;
import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.Canal;
import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AporteController.class)
class AporteControllerTest {

    @Autowired MockMvc mvc;

    @MockBean RegistrarAporteUseCase registrarAporteUseCase;
    @MockBean ConsultarAportesUseCase consultarAportesUseCase;

    private Aporte aporte(Long id, boolean revision) {
        return new Aporte(id, "AF-001", new BigDecimal("1000.00"),
                LocalDate.of(2025, 3, 10), Canal.WEB, "2025-03", revision, "key-1");
    }

    private String body() {
        return """
            {"afiliadoId":"AF-001","monto":1000.00,"fecha":"2025-03-10",
             "canal":"WEB","idempotenciaKey":"key-1"}
            """;
    }

    @Test
    void registrar_devuelve_201_con_location() throws Exception {
        when(registrarAporteUseCase.registrar(any())).thenReturn(aporte(42L, false));

        mvc.perform(post("/api/aportes").contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/aportes/42")))
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.marcadaRevision").value(false));
    }

    @Test
    void tope_excedido_devuelve_422_problem_detail() throws Exception {
        when(registrarAporteUseCase.registrar(any()))
                .thenThrow(new TopeMensualExcedidoException("El aporte supera el tope mensual permitido para el afiliado."));

        mvc.perform(post("/api/aportes").contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Regla de negocio no cumplida"))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void conflicto_concurrencia_devuelve_409() throws Exception {
        when(registrarAporteUseCase.registrar(any()))
                .thenThrow(new ConflictoConcurrenciaException("Reintente la operación."));

        mvc.perform(post("/api/aportes").contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflicto de concurrencia"));
    }

    @Test
    void payload_invalido_devuelve_400() throws Exception {
        // monto negativo y canal inexistente
        String malo = """
            {"afiliadoId":"","monto":-5,"fecha":"2025-03-10","canal":"WEB","idempotenciaKey":"k"}
            """;
        mvc.perform(post("/api/aportes").contentType(MediaType.APPLICATION_JSON).content(malo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Datos de entrada inválidos"));
    }

    @Test
    void canal_invalido_devuelve_400() throws Exception {
        String malo = """
            {"afiliadoId":"AF-001","monto":1000,"fecha":"2025-03-10","canal":"TELEGRAMA","idempotenciaKey":"k"}
            """;
        mvc.perform(post("/api/aportes").contentType(MediaType.APPLICATION_JSON).content(malo))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aporte_inexistente_devuelve_404() throws Exception {
        when(consultarAportesUseCase.buscarPorId(any()))
                .thenThrow(new RecursoNoEncontradoException("No existe un aporte con el id indicado."));

        mvc.perform(get("/api/aportes/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }
}
