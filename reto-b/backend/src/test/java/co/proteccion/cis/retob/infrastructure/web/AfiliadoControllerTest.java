package co.proteccion.cis.retob.infrastructure.web;

import co.proteccion.cis.retob.domain.exception.AfiliadoNotFoundException;
import co.proteccion.cis.retob.domain.model.Afiliado;
import co.proteccion.cis.retob.domain.model.EstadoAfiliado;
import co.proteccion.cis.retob.domain.port.in.ConsultarAfiliadoUseCase;
import co.proteccion.cis.retob.domain.port.in.RegistrarAfiliadoUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AfiliadoController.class)
@DisplayName("AfiliadoController — endpoints y manejo de errores")
class AfiliadoControllerTest {

    @Autowired MockMvc mvc;

    @MockBean RegistrarAfiliadoUseCase  registrarUseCase;
    @MockBean ConsultarAfiliadoUseCase  consultarUseCase;

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Afiliado afiliadoStub(String id) {
        return new Afiliado(1L, id, "Juan Sintético", EstadoAfiliado.ACTIVO, OffsetDateTime.now());
    }

    // ── POST /api/afiliados ───────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/afiliados — registrar")
    class Registrar {

        @Test
        @DisplayName("afiliado nuevo devuelve 201 con estado ACTIVO")
        void registrar_exitoso() throws Exception {
            when(registrarUseCase.registrar(any())).thenReturn(afiliadoStub("AF-001"));

            mvc.perform(post("/api/afiliados").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"afiliadoId":"AF-001","nombre":"Juan Sintético"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.afiliadoId").value("AF-001"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
        }

        @Test
        @DisplayName("afiliadoId duplicado devuelve 409 con CONFLICTO")
        void afiliado_duplicado() throws Exception {
            when(registrarUseCase.registrar(any()))
                    .thenThrow(new IllegalArgumentException("Ya existe un afiliado con id: AF-001"));

            mvc.perform(post("/api/afiliados").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"afiliadoId":"AF-001","nombre":"Otro nombre"}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICTO"))
                .andExpect(jsonPath("$.mensaje").value(containsString("AF-001")));
        }

        @Test
        @DisplayName("afiliadoId vacío devuelve 400 con VALIDACION_FALLIDA")
        void afiliado_id_vacio() throws Exception {
            mvc.perform(post("/api/afiliados").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"afiliadoId":"","nombre":"Juan"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.campos.afiliadoId").exists());
        }

        @Test
        @DisplayName("nombre vacío devuelve 400 con campo nombre en el error")
        void nombre_vacio() throws Exception {
            mvc.perform(post("/api/afiliados").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"afiliadoId":"AF-002","nombre":""}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nombre").exists());
        }
    }

    // ── GET /api/afiliados/{afiliadoId} ───────────────────────────────────────

    @Nested
    @DisplayName("GET /api/afiliados/{afiliadoId} — consultar")
    class Consultar {

        @Test
        @DisplayName("afiliado existente devuelve 200 con datos completos")
        void consultar_existente() throws Exception {
            when(consultarUseCase.consultar(eq("AF-001"))).thenReturn(afiliadoStub("AF-001"));

            mvc.perform(get("/api/afiliados/AF-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.afiliadoId").value("AF-001"))
                .andExpect(jsonPath("$.nombre").value("Juan Sintético"));
        }

        @Test
        @DisplayName("afiliado no existente devuelve 404 con AFILIADO_NO_ENCONTRADO")
        void consultar_no_encontrado() throws Exception {
            when(consultarUseCase.consultar(eq("AF-999")))
                    .thenThrow(new AfiliadoNotFoundException("AF-999"));

            mvc.perform(get("/api/afiliados/AF-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("AFILIADO_NO_ENCONTRADO"))
                .andExpect(jsonPath("$.mensaje").value(containsString("AF-999")));
        }
    }

    // ── GET /api/afiliados ────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/afiliados — listar todos")
    class ListarTodos {

        @Test
        @DisplayName("devuelve lista con todos los afiliados")
        void listar_exitoso() throws Exception {
            when(consultarUseCase.consultarTodos())
                    .thenReturn(List.of(afiliadoStub("AF-001"), afiliadoStub("AF-002")));

            mvc.perform(get("/api/afiliados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("lista vacía devuelve 200 con array vacío")
        void lista_vacia() throws Exception {
            when(consultarUseCase.consultarTodos()).thenReturn(List.of());

            mvc.perform(get("/api/afiliados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        }
    }
}
