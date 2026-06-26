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

    static final String APORTE_UUID = "550e8400-e29b-41d4-a716-446655440001";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @MockBean RegistrarAporteUseCase     registrarUseCase;
    @MockBean ConsultarAportesUseCase    consultarUseCase;
    @MockBean CambiarEstadoAporteUseCase cambiarEstadoUseCase;
    @MockBean ConsultarRevisionesUseCase consultarRevisionesUseCase;

    private Aporte aporteStub(EstadoAporte estado) {
        return new Aporte(APORTE_UUID, "AF-001", new BigDecimal("1000000"),
                LocalDate.of(2026, 6, 26), CanalOrigen.APP_MOVIL, "2026-06", estado, "k-001");
    }

    private ConsolidadoAportes consolidadoStub() {
        return new ConsolidadoAportes("AF-001", "2026-01", "2026-06",
                new BigDecimal("1000000"), List.of(aporteStub(EstadoAporte.PENDIENTE)));
    }

    // ── POST /api/aportes ────────────────────────────────────────────────────

    @Nested @DisplayName("POST /api/aportes")
    class RegistrarAporte {

        @Test @DisplayName("válido → 201 con estado PENDIENTE")
        void registrar_pendiente() throws Exception {
            when(registrarUseCase.registrar(any())).thenReturn(aporteStub(EstadoAporte.PENDIENTE));

            mvc.perform(post("/api/aportes").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"afiliadoId":"AF-001","monto":1000000,"canal":"APP_MOVIL","idempotenciaKey":"k-001"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.id").value(APORTE_UUID));
        }

        @Test @DisplayName("monto sobre umbral → 201 con estado EN_REVISION")
        void registrar_en_revision() throws Exception {
            when(registrarUseCase.registrar(any())).thenReturn(aporteStub(EstadoAporte.EN_REVISION));

            mvc.perform(post("/api/aportes").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"afiliadoId":"AF-001","monto":6000000,"canal":"WEB","idempotenciaKey":"k-002"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("EN_REVISION"));
        }

        @Test @DisplayName("monto=0 → 400 VALIDACION_FALLIDA")
        void monto_cero() throws Exception {
            mvc.perform(post("/api/aportes").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"afiliadoId":"AF-001","monto":0,"canal":"APP_MOVIL","idempotenciaKey":"k-x"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.campos.monto").exists());
        }

        @Test @DisplayName("canal inválido → 400 FORMATO_INVALIDO")
        void canal_invalido() throws Exception {
            mvc.perform(post("/api/aportes").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"afiliadoId":"AF-001","monto":100,"canal":"TELEGRAMA","idempotenciaKey":"k-x"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("FORMATO_INVALIDO"));
        }

        @Test @DisplayName("afiliado no encontrado → 404 AFILIADO_NO_ENCONTRADO")
        void afiliado_no_encontrado() throws Exception {
            when(registrarUseCase.registrar(any())).thenThrow(new AfiliadoNotFoundException("AF-999"));

            mvc.perform(post("/api/aportes").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"afiliadoId":"AF-999","monto":1000000,"canal":"WEB","idempotenciaKey":"k-x"}
                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("AFILIADO_NO_ENCONTRADO"));
        }

        @Test @DisplayName("tope excedido → 422 TOPE_MENSUAL_EXCEDIDO")
        void tope_excedido() throws Exception {
            when(registrarUseCase.registrar(any())).thenThrow(
                    new TopeMensualExcedidoException(new BigDecimal("10000000"),
                            new BigDecimal("9500000"), new BigDecimal("1000000")));

            mvc.perform(post("/api/aportes").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"afiliadoId":"AF-001","monto":1000000,"canal":"APP_MOVIL","idempotenciaKey":"k-x"}
                        """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("TOPE_MENSUAL_EXCEDIDO"));
        }
    }

    // ── GET /api/aportes/consolidado ──────────────────────────────────────────

    @Nested @DisplayName("GET /api/aportes/consolidado")
    class Consolidado {

        @Test @DisplayName("parámetros válidos → 200 con total")
        void consolidado_exitoso() throws Exception {
            when(consultarUseCase.consultar(any())).thenReturn(consolidadoStub());

            mvc.perform(get("/api/aportes/consolidado")
                    .param("afiliadoId", "AF-001")
                    .param("periodoDesde", "2026-01")
                    .param("periodoHasta", "2026-06"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAportado").value(1000000))
                .andExpect(jsonPath("$.detalle", hasSize(1)));
        }

        @Test @DisplayName("periodo con formato incorrecto → 400 VALIDACION_FALLIDA")
        void periodo_invalido() throws Exception {
            mvc.perform(get("/api/aportes/consolidado")
                    .param("afiliadoId", "AF-001")
                    .param("periodoDesde", "2026-1")
                    .param("periodoHasta", "2026-06"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDACION_FALLIDA"));
        }
    }

    // ── PATCH /api/aportes/{id}/estado ────────────────────────────────────────

    @Nested @DisplayName("PATCH /api/aportes/{id}/estado")
    class CambiarEstado {

        @Test @DisplayName("transición válida → 200 con nuevo estado")
        void exitoso() throws Exception {
            when(cambiarEstadoUseCase.cambiar(any())).thenReturn(aporteStub(EstadoAporte.APROBADO));

            mvc.perform(patch("/api/aportes/" + APORTE_UUID + "/estado")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"nuevoEstado":"APROBADO","revisor":"OP-01","comentario":"OK"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADO"));
        }

        @Test @DisplayName("UUID inexistente → 404 APORTE_NO_ENCONTRADO")
        void not_found() throws Exception {
            when(cambiarEstadoUseCase.cambiar(any()))
                    .thenThrow(new AporteNotFoundException("no-existe-uuid"));

            mvc.perform(patch("/api/aportes/no-existe-uuid/estado")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"nuevoEstado":"APROBADO","revisor":"OP-01","comentario":"X"}
                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("APORTE_NO_ENCONTRADO"));
        }

        @Test @DisplayName("transición inválida → 400 TRANSICION_INVALIDA")
        void transicion_invalida() throws Exception {
            when(cambiarEstadoUseCase.cambiar(any()))
                    .thenThrow(new TransicionEstadoInvalidaException(EstadoAporte.APROBADO, EstadoAporte.EN_REVISION));

            mvc.perform(patch("/api/aportes/" + APORTE_UUID + "/estado")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"nuevoEstado":"EN_REVISION","revisor":"OP-01","comentario":"X"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("TRANSICION_INVALIDA"));
        }

        @Test @DisplayName("revisor vacío → 400 VALIDACION_FALLIDA")
        void revisor_vacio() throws Exception {
            mvc.perform(patch("/api/aportes/" + APORTE_UUID + "/estado")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"nuevoEstado":"APROBADO","revisor":"","comentario":"X"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.revisor").exists());
        }
    }

    // ── GET /api/aportes/{id}/revisiones ──────────────────────────────────────

    @Nested @DisplayName("GET /api/aportes/{id}/revisiones")
    class Revisiones {

        @Test @DisplayName("devuelve lista de revisiones")
        void revisiones_exitoso() throws Exception {
            when(consultarRevisionesUseCase.consultar(APORTE_UUID)).thenReturn(List.of(
                    new RevisionAporte("rev-uuid-1", APORTE_UUID, "OP-01",
                            EstadoAporte.APROBADO, "OK", OffsetDateTime.now())
            ));

            mvc.perform(get("/api/aportes/" + APORTE_UUID + "/revisiones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].revisor").value("OP-01"))
                .andExpect(jsonPath("$[0].decision").value("APROBADO"));
        }

        @Test @DisplayName("sin revisiones → 200 array vacío")
        void sin_revisiones() throws Exception {
            when(consultarRevisionesUseCase.consultar(any())).thenReturn(List.of());

            mvc.perform(get("/api/aportes/" + APORTE_UUID + "/revisiones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        }
    }
}
