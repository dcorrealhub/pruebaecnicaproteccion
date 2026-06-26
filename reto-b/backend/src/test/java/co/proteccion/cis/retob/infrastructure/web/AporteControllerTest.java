package co.proteccion.cis.retob.infrastructure.web;

import co.proteccion.cis.retob.domain.exception.AfiliadoNotFoundException;
import co.proteccion.cis.retob.domain.exception.AporteNotFoundException;
import co.proteccion.cis.retob.domain.exception.TopeMensualExcedidoException;
import co.proteccion.cis.retob.domain.exception.TransicionEstadoInvalidaException;
import co.proteccion.cis.retob.domain.model.*;
import co.proteccion.cis.retob.domain.port.in.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AporteController.class)
@DisplayName("AporteController — endpoints y manejo de errores")
class AporteControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @MockBean RegistrarAporteUseCase    registrarUseCase;
    @MockBean ConsultarAportesUseCase   consultarUseCase;
    @MockBean CambiarEstadoAporteUseCase cambiarEstadoUseCase;
    @MockBean ConsultarRevisionesUseCase consultarRevisionesUseCase;

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Aporte aporteStub(EstadoAporte estado) {
        return new Aporte(1L, "AF-001", new BigDecimal("1000000"),
                LocalDate.of(2026, 6, 26), CanalOrigen.APP_MOVIL, "2026-06", estado, "k-001");
    }

    private ConsolidadoAportes consolidadoStub() {
        return new ConsolidadoAportes("AF-001", "2026-01", "2026-06",
                new BigDecimal("1000000"), List.of(aporteStub(EstadoAporte.PENDIENTE)));
    }

    private String body(Object obj) throws Exception { return mapper.writeValueAsString(obj); }

    // ── POST /api/aportes ────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/aportes — registrar aporte")
    class RegistrarAporte {

        @Test
        @DisplayName("aporte válido devuelve 201 con estado PENDIENTE")
        void registrar_pendiente() throws Exception {
            when(registrarUseCase.registrar(any())).thenReturn(aporteStub(EstadoAporte.PENDIENTE));

            mvc.perform(post("/api/aportes").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"afiliadoId":"AF-001","monto":1000000,"canal":"APP_MOVIL","idempotenciaKey":"k-001"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @DisplayName("aporte sobre umbral devuelve 201 con estado EN_REVISION")
        void registrar_en_revision() throws Exception {
            when(registrarUseCase.registrar(any())).thenReturn(aporteStub(EstadoAporte.EN_REVISION));

            mvc.perform(post("/api/aportes").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"afiliadoId":"AF-001","monto":6000000,"canal":"WEB","idempotenciaKey":"k-002"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("EN_REVISION"));
        }

        @Test
        @DisplayName("monto cero devuelve 400 con VALIDACION_FALLIDA")
        void monto_cero_es_invalido() throws Exception {
            mvc.perform(post("/api/aportes").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"afiliadoId":"AF-001","monto":0,"canal":"APP_MOVIL","idempotenciaKey":"k-x"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.campos.monto").exists());
        }

        @Test
        @DisplayName("afiliadoId vacío devuelve 400 con campo en campos[]")
        void afiliado_id_vacio() throws Exception {
            mvc.perform(post("/api/aportes").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"afiliadoId":"","monto":1000000,"canal":"APP_MOVIL","idempotenciaKey":"k-x"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.afiliadoId").exists());
        }

        @Test
        @DisplayName("canal con valor inválido devuelve 400 con FORMATO_INVALIDO")
        void canal_invalido() throws Exception {
            mvc.perform(post("/api/aportes").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"afiliadoId":"AF-001","monto":1000000,"canal":"INVALIDO","idempotenciaKey":"k-x"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("FORMATO_INVALIDO"));
        }

        @Test
        @DisplayName("afiliado no encontrado devuelve 404 con AFILIADO_NO_ENCONTRADO")
        void afiliado_no_encontrado() throws Exception {
            when(registrarUseCase.registrar(any()))
                    .thenThrow(new AfiliadoNotFoundException("AF-999"));

            mvc.perform(post("/api/aportes").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"afiliadoId":"AF-999","monto":1000000,"canal":"WEB","idempotenciaKey":"k-x"}
                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("AFILIADO_NO_ENCONTRADO"));
        }

        @Test
        @DisplayName("tope mensual excedido devuelve 422 con TOPE_MENSUAL_EXCEDIDO")
        void tope_excedido() throws Exception {
            when(registrarUseCase.registrar(any()))
                    .thenThrow(new TopeMensualExcedidoException(
                            new BigDecimal("10000000"), new BigDecimal("9500000"), new BigDecimal("1000000")));

            mvc.perform(post("/api/aportes").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"afiliadoId":"AF-001","monto":1000000,"canal":"APP_MOVIL","idempotenciaKey":"k-x"}
                        """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("TOPE_MENSUAL_EXCEDIDO"));
        }
    }

    // ── GET /api/aportes/consolidado ──────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/aportes/consolidado")
    class Consolidado {

        @Test
        @DisplayName("parámetros válidos devuelven 200 con total y detalle")
        void consolidado_exitoso() throws Exception {
            when(consultarUseCase.consultar(any())).thenReturn(consolidadoStub());

            mvc.perform(get("/api/aportes/consolidado")
                    .param("afiliadoId", "AF-001")
                    .param("periodoDesde", "2026-01")
                    .param("periodoHasta", "2026-06"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.afiliadoId").value("AF-001"))
                .andExpect(jsonPath("$.totalAportado").value(1000000))
                .andExpect(jsonPath("$.detalle", hasSize(1)));
        }

        @Test
        @DisplayName("periodo con formato incorrecto devuelve 400 VALIDACION_FALLIDA")
        void periodo_formato_incorrecto() throws Exception {
            mvc.perform(get("/api/aportes/consolidado")
                    .param("afiliadoId", "AF-001")
                    .param("periodoDesde", "2026-1")   // falta el 0
                    .param("periodoHasta", "2026-06"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDACION_FALLIDA"));
        }
    }

    // ── PATCH /api/aportes/{id}/estado ────────────────────────────────────────

    @Nested
    @DisplayName("PATCH /api/aportes/{id}/estado — cambiar estado")
    class CambiarEstado {

        @Test
        @DisplayName("transición válida devuelve 200 con nuevo estado")
        void cambiar_estado_exitoso() throws Exception {
            when(cambiarEstadoUseCase.cambiar(any())).thenReturn(aporteStub(EstadoAporte.APROBADO));

            mvc.perform(patch("/api/aportes/1/estado").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"nuevoEstado":"APROBADO","revisor":"OP-01","comentario":"OK"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADO"));
        }

        @Test
        @DisplayName("aporte no encontrado devuelve 404 con APORTE_NO_ENCONTRADO")
        void aporte_no_encontrado() throws Exception {
            when(cambiarEstadoUseCase.cambiar(any()))
                    .thenThrow(new AporteNotFoundException(99L));

            mvc.perform(patch("/api/aportes/99/estado").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"nuevoEstado":"APROBADO","revisor":"OP-01","comentario":"X"}
                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("APORTE_NO_ENCONTRADO"));
        }

        @Test
        @DisplayName("transición inválida devuelve 400 con TRANSICION_INVALIDA")
        void transicion_invalida() throws Exception {
            when(cambiarEstadoUseCase.cambiar(any()))
                    .thenThrow(new TransicionEstadoInvalidaException(EstadoAporte.APROBADO, EstadoAporte.EN_REVISION));

            mvc.perform(patch("/api/aportes/1/estado").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"nuevoEstado":"EN_REVISION","revisor":"OP-01","comentario":"X"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("TRANSICION_INVALIDA"));
        }

        @Test
        @DisplayName("id no numérico devuelve 400 con TIPO_INVALIDO")
        void id_no_numerico() throws Exception {
            mvc.perform(patch("/api/aportes/abc/estado").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"nuevoEstado":"APROBADO","revisor":"OP-01","comentario":"X"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("TIPO_INVALIDO"))
                .andExpect(jsonPath("$.mensaje").value(containsString("id")));
        }

        @Test
        @DisplayName("revisor vacío devuelve 400 con VALIDACION_FALLIDA")
        void revisor_vacio() throws Exception {
            mvc.perform(patch("/api/aportes/1/estado").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"nuevoEstado":"APROBADO","revisor":"","comentario":"X"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.revisor").exists());
        }
    }

    // ── GET /api/aportes/{id}/revisiones ──────────────────────────────────────

    @Nested
    @DisplayName("GET /api/aportes/{id}/revisiones")
    class Revisiones {

        @Test
        @DisplayName("devuelve lista de revisiones con revisor y decisión")
        void revisiones_exitoso() throws Exception {
            when(consultarRevisionesUseCase.consultar(1L)).thenReturn(List.of(
                    new RevisionAporte(1L, 1L, "OP-01", EstadoAporte.APROBADO, "OK", OffsetDateTime.now())
            ));

            mvc.perform(get("/api/aportes/1/revisiones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].revisor").value("OP-01"))
                .andExpect(jsonPath("$[0].decision").value("APROBADO"));
        }

        @Test
        @DisplayName("lista vacía devuelve 200 con array vacío")
        void sin_revisiones() throws Exception {
            when(consultarRevisionesUseCase.consultar(any())).thenReturn(List.of());

            mvc.perform(get("/api/aportes/2/revisiones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        }
    }
}
