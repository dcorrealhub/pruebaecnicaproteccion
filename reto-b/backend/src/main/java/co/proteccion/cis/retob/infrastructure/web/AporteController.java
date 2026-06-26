package co.proteccion.cis.retob.infrastructure.web;

import co.proteccion.cis.retob.domain.port.in.CambiarEstadoAporteUseCase;
import co.proteccion.cis.retob.domain.port.in.CambiarEstadoAporteUseCase.CambiarEstadoCommand;
import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase;
import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase.ConsultarAportesQuery;
import co.proteccion.cis.retob.domain.port.in.ConsultarRevisionesUseCase;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase.RegistrarAporteCommand;
import co.proteccion.cis.retob.infrastructure.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/aportes")
@RequiredArgsConstructor
@Validated
@Tag(name = "Aportes", description = "Registro, consulta y gestión del ciclo de vida de aportes voluntarios")
public class AporteController {

    private final RegistrarAporteUseCase     registrarAporteUseCase;
    private final ConsultarAportesUseCase    consultarAportesUseCase;
    private final CambiarEstadoAporteUseCase cambiarEstadoAporteUseCase;
    private final ConsultarRevisionesUseCase consultarRevisionesUseCase;

    @Operation(summary = "Registrar un aporte voluntario",
            description = """
                    Idempotente por `idempotenciaKey`. Estado resultante:
                    - `PENDIENTE` si monto ≤ umbral de revisión
                    - `EN_REVISION` si monto > umbral
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Aporte registrado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(example = "{\"error\":\"VALIDACION_FALLIDA\"}"))),
            @ApiResponse(responseCode = "404", description = "Afiliado no encontrado"),
            @ApiResponse(responseCode = "422", description = "Tope mensual excedido"),
            @ApiResponse(responseCode = "409", description = "Conflicto de concurrencia")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AporteResponse registrar(@Valid @RequestBody RegistrarAporteRequest req) {
        return AporteResponse.from(registrarAporteUseCase.registrar(
                new RegistrarAporteCommand(req.afiliadoId(), req.monto(), req.canal(), req.idempotenciaKey())));
    }

    @Operation(summary = "Consolidado de aportes por periodo",
            description = "Total aportado y detalle de aportes en el rango YYYY-MM indicado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consolidado calculado"),
            @ApiResponse(responseCode = "400", description = "Formato de periodo inválido")
    })
    @GetMapping("/consolidado")
    public ConsolidadoResponse consolidado(
            @Parameter(description = "ID sintético del afiliado", example = "AF-001")
            @NotBlank(message = "El afiliadoId es obligatorio")
            @RequestParam String afiliadoId,

            @Parameter(description = "Periodo inicial (YYYY-MM)", example = "2026-01")
            @Pattern(regexp = "\\d{4}-\\d{2}", message = "El periodo debe tener formato YYYY-MM")
            @RequestParam String periodoDesde,

            @Parameter(description = "Periodo final (YYYY-MM)", example = "2026-06")
            @Pattern(regexp = "\\d{4}-\\d{2}", message = "El periodo debe tener formato YYYY-MM")
            @RequestParam String periodoHasta) {
        return ConsolidadoResponse.from(consultarAportesUseCase.consultar(
                new ConsultarAportesQuery(afiliadoId, periodoDesde, periodoHasta)));
    }

    @Operation(summary = "Cambiar estado de un aporte",
            description = """
                    Transiciones válidas:
                    - `PENDIENTE` → `EN_REVISION` o `APROBADO`
                    - `EN_REVISION` → `APROBADO` o `RECHAZADO`
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado"),
            @ApiResponse(responseCode = "400", description = "Transición inválida o datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Aporte no encontrado")
    })
    @PatchMapping("/{id}/estado")
    public AporteResponse cambiarEstado(
            @Parameter(description = "UUID del aporte", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String id,
            @Valid @RequestBody CambiarEstadoRequest req) {
        return AporteResponse.from(cambiarEstadoAporteUseCase.cambiar(
                new CambiarEstadoCommand(id, req.nuevoEstado(), req.revisor(), req.comentario())));
    }

    @Operation(summary = "Historial de revisiones de un aporte")
    @ApiResponse(responseCode = "200", description = "Historial retornado")
    @GetMapping("/{id}/revisiones")
    public List<RevisionResponse> revisiones(
            @Parameter(description = "UUID del aporte", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String id) {
        return consultarRevisionesUseCase.consultar(id).stream()
                .map(RevisionResponse::from).toList();
    }
}
