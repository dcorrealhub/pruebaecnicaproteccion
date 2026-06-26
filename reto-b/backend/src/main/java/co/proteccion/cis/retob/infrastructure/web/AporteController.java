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

    private final RegistrarAporteUseCase registrarAporteUseCase;
    private final ConsultarAportesUseCase consultarAportesUseCase;
    private final CambiarEstadoAporteUseCase cambiarEstadoAporteUseCase;
    private final ConsultarRevisionesUseCase consultarRevisionesUseCase;

    @Operation(
            summary = "Registrar un aporte voluntario",
            description = """
                    Registra un aporte al fondo voluntario. La operación es **idempotente**:
                    si se reenvía la misma `idempotenciaKey`, se retorna el aporte original sin duplicar.

                    El estado resultante es:
                    - `PENDIENTE` si el monto no supera el umbral de revisión
                    - `EN_REVISION` si el monto supera el umbral configurado en parámetros
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Aporte registrado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(example = "{\"error\":\"VALIDACION_FALLIDA\",\"mensaje\":\"...\"}"))),
            @ApiResponse(responseCode = "404", description = "Afiliado no encontrado",
                    content = @Content(schema = @Schema(example = "{\"error\":\"AFILIADO_NO_ENCONTRADO\",\"mensaje\":\"...\"}"))),
            @ApiResponse(responseCode = "422", description = "Tope mensual excedido",
                    content = @Content(schema = @Schema(example = "{\"error\":\"TOPE_MENSUAL_EXCEDIDO\",\"mensaje\":\"...\"}"))),
            @ApiResponse(responseCode = "409", description = "Conflicto de concurrencia en saldo mensual",
                    content = @Content(schema = @Schema(example = "{\"error\":\"CONFLICTO_CONCURRENCIA\",\"mensaje\":\"...\"}")))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AporteResponse registrar(@Valid @RequestBody RegistrarAporteRequest req) {
        var command = new RegistrarAporteCommand(
                req.afiliadoId(),
                req.monto(),
                req.canal(),
                req.idempotenciaKey()
        );
        return AporteResponse.from(registrarAporteUseCase.registrar(command));
    }

    @Operation(
            summary = "Consolidado de aportes por periodo",
            description = "Retorna el total aportado y el detalle de cada aporte de un afiliado en el rango de periodos indicado (formato `YYYY-MM`)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consolidado calculado correctamente"),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos",
                    content = @Content(schema = @Schema(example = "{\"error\":\"VALIDACION_FALLIDA\",\"campos\":{\"periodoDesde\":\"...\"}}")))
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
        var query = new ConsultarAportesQuery(afiliadoId, periodoDesde, periodoHasta);
        return ConsolidadoResponse.from(consultarAportesUseCase.consultar(query));
    }

    @Operation(
            summary = "Cambiar estado de un aporte",
            description = """
                    Aplica una transición de estado al aporte e inserta un registro inmutable
                    en el historial de revisiones.

                    Transiciones válidas:
                    - `PENDIENTE` → `EN_REVISION` o `APROBADO`
                    - `EN_REVISION` → `APROBADO` o `RECHAZADO`
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Transición de estado inválida o datos inválidos",
                    content = @Content(schema = @Schema(example = "{\"error\":\"TRANSICION_INVALIDA\",\"mensaje\":\"...\"}"))),
            @ApiResponse(responseCode = "404", description = "Aporte no encontrado",
                    content = @Content(schema = @Schema(example = "{\"error\":\"APORTE_NO_ENCONTRADO\",\"mensaje\":\"...\"}")))
    })
    @PatchMapping("/{id}/estado")
    public AporteResponse cambiarEstado(
            @Parameter(description = "ID del aporte", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody CambiarEstadoRequest req) {
        var command = new CambiarEstadoCommand(id, req.nuevoEstado(), req.revisor(), req.comentario());
        return AporteResponse.from(cambiarEstadoAporteUseCase.cambiar(command));
    }

    @Operation(
            summary = "Historial de revisiones de un aporte",
            description = "Retorna todos los cambios de estado realizados sobre el aporte, ordenados cronológicamente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial retornado correctamente"),
            @ApiResponse(responseCode = "404", description = "Aporte no encontrado")
    })
    @GetMapping("/{id}/revisiones")
    public List<RevisionResponse> revisiones(
            @Parameter(description = "ID del aporte", example = "1")
            @PathVariable Long id) {
        return consultarRevisionesUseCase.consultar(id)
                .stream()
                .map(RevisionResponse::from)
                .toList();
    }
}
