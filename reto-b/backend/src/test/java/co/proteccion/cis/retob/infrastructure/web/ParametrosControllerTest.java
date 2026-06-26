package co.proteccion.cis.retob.infrastructure.web;

import co.proteccion.cis.retob.domain.model.ParametrosFondo;
import co.proteccion.cis.retob.domain.port.in.ActualizarParametrosUseCase;
import co.proteccion.cis.retob.domain.port.in.ConsultarParametrosUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ParametrosController.class)
@DisplayName("ParametrosController — endpoints y manejo de errores")
class ParametrosControllerTest {

    @Autowired MockMvc mvc;

    @MockBean ConsultarParametrosUseCase  consultarUseCase;
    @MockBean ActualizarParametrosUseCase actualizarUseCase;

    private ParametrosFondo parametrosStub() {
        return new ParametrosFondo("param-uuid-001",
                new BigDecimal("10000"),
                new BigDecimal("10000000"),
                new BigDecimal("5000000"),
                "SYSTEM", OffsetDateTime.now(), "Carga inicial");
    }

    @Nested
    @DisplayName("GET /api/parametros/actual")
    class ConsultarActual {

        @Test
        @DisplayName("con datos devuelve 200 y los valores vigentes")
        void con_datos() throws Exception {
            when(consultarUseCase.consultarActual()).thenReturn(Optional.of(parametrosStub()));

            mvc.perform(get("/api/parametros/actual"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montoMinimo").value(10000))
                .andExpect(jsonPath("$.topeMensual").value(10000000))
                .andExpect(jsonPath("$.umbralRevision").value(5000000))
                .andExpect(jsonPath("$.modificadoPor").value("SYSTEM"));
        }

        @Test
        @DisplayName("tabla vacía devuelve 204 No Content")
        void sin_datos_204() throws Exception {
            when(consultarUseCase.consultarActual()).thenReturn(Optional.empty());

            mvc.perform(get("/api/parametros/actual"))
                .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("GET /api/parametros/historial")
    class ConsultarHistorial {

        @Test
        @DisplayName("devuelve lista con todos los registros históricos")
        void historial_con_registros() throws Exception {
            ParametrosFondo p2 = new ParametrosFondo("param-uuid-002",
                    new BigDecimal("10000"),
                    new BigDecimal("12000000"),
                    new BigDecimal("6000000"),
                    "ADMIN", OffsetDateTime.now(), "Ajuste Q2");
            when(consultarUseCase.consultarHistorial()).thenReturn(List.of(parametrosStub(), p2));

            mvc.perform(get("/api/parametros/historial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].modificadoPor").value("ADMIN"));
        }

        @Test
        @DisplayName("historial vacío devuelve 200 con array vacío")
        void historial_vacio() throws Exception {
            when(consultarUseCase.consultarHistorial()).thenReturn(List.of());

            mvc.perform(get("/api/parametros/historial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("POST /api/parametros — actualizar")
    class Actualizar {

        @Test
        @DisplayName("parámetros válidos devuelven 201 con el nuevo registro")
        void actualizar_exitoso() throws Exception {
            ParametrosFondo nuevo = new ParametrosFondo("param-uuid-003",
                    new BigDecimal("10000"),
                    new BigDecimal("12000000"),
                    new BigDecimal("4000000"),
                    "ADMIN", OffsetDateTime.now(), "Ajuste Q3");
            when(actualizarUseCase.actualizar(any())).thenReturn(nuevo);

            mvc.perform(post("/api/parametros").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"montoMinimo":10000,"topeMensual":12000000,"umbralRevision":4000000,
                         "modificadoPor":"ADMIN","comentario":"Ajuste Q3"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.montoMinimo").value(10000))
                .andExpect(jsonPath("$.topeMensual").value(12000000))
                .andExpect(jsonPath("$.modificadoPor").value("ADMIN"));
        }

        @Test
        @DisplayName("umbral >= tope lanza IllegalArgumentException → 409 CONFLICTO")
        void umbral_mayor_que_tope() throws Exception {
            when(actualizarUseCase.actualizar(any()))
                    .thenThrow(new IllegalArgumentException(
                            "El umbral de revisión (6000000) debe ser menor al tope mensual (5000000)."));

            mvc.perform(post("/api/parametros").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"montoMinimo":10000,"topeMensual":5000000,"umbralRevision":6000000,
                         "modificadoPor":"ADMIN","comentario":"error"}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICTO"))
                .andExpect(jsonPath("$.mensaje").value(containsString("umbral")));
        }

        @Test
        @DisplayName("topeMensual nulo devuelve 400 con VALIDACION_FALLIDA")
        void tope_nulo() throws Exception {
            mvc.perform(post("/api/parametros").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"montoMinimo":10000,"umbralRevision":3000000,"modificadoPor":"ADMIN","comentario":"X"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.topeMensual").exists());
        }

        @Test
        @DisplayName("montoMinimo nulo devuelve 400 con campo en el error")
        void monto_minimo_nulo() throws Exception {
            mvc.perform(post("/api/parametros").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"topeMensual":10000000,"umbralRevision":5000000,"modificadoPor":"ADMIN","comentario":"X"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.montoMinimo").exists());
        }

        @Test
        @DisplayName("modificadoPor vacío devuelve 400 con campo en el error")
        void modificado_por_vacio() throws Exception {
            mvc.perform(post("/api/parametros").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"montoMinimo":10000,"topeMensual":10000000,"umbralRevision":5000000,"modificadoPor":"","comentario":"X"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.modificadoPor").exists());
        }
    }
}
