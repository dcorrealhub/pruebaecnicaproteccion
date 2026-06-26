package co.proteccion.cis.retob.infrastructure.web;

import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase;
import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase.ConsultarAportesQuery;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase.RegistrarAporteCommand;
import co.proteccion.cis.retob.infrastructure.web.dto.AporteResponse;
import co.proteccion.cis.retob.infrastructure.web.dto.ConsolidadoResponse;
import co.proteccion.cis.retob.infrastructure.web.dto.RegistrarAporteRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/aportes")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "Aportes Voluntarios", description = "Registro y consulta de aportes voluntarios a fondos de pensiones")
public class AporteController {

    private final RegistrarAporteUseCase registrarAporteUseCase;
    private final ConsultarAportesUseCase consultarAportesUseCase;

    @Operation(
            summary = "Registrar aporte voluntario",
            description = "Registra un aporte voluntario de un afiliado. La operacion es idempotente: " +
                    "reintentos con la misma idempotenciaKey retornan el aporte original sin duplicarlo. " +
                    "Si el monto supera el umbral de revision (configurable), el aporte se marca con " +
                    "marcadaRevision=true para revision posterior del equipo de cumplimiento."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Aporte registrado exitosamente",
                    content = @Content(schema = @Schema(implementation = AporteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada invalidos (campo faltante o formato incorrecto)",
                    content = @Content(schema = @Schema(example = "{\"error\":\"Datos de entrada invalidos\",\"campos\":{\"monto\":\"El monto debe ser mayor a cero\"}}"))),
            @ApiResponse(responseCode = "409", description = "Conflicto de concurrencia optimista — reintente la operacion",
                    content = @Content(schema = @Schema(example = "{\"error\":\"Conflicto de concurrencia, reintente la operacion\"}"))),
            @ApiResponse(responseCode = "422", description = "Regla de negocio violada: monto no positivo o tope mensual superado",
                    content = @Content(schema = @Schema(example = "{\"error\":\"El aporte supera el tope mensual permitido de 10000000\"}")))
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
            summary = "Consultar consolidado de aportes",
            description = "Retorna el total aportado y el detalle de cada aporte de un afiliado " +
                    "en el rango de periodos indicado (formato YYYY-MM). " +
                    "Si no hay aportes en el periodo, retorna total=0 y detalle vacio."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consolidado obtenido exitosamente",
                    content = @Content(schema = @Schema(implementation = ConsolidadoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parametros de consulta invalidos o faltantes",
                    content = @Content(schema = @Schema(example = "{\"error\":\"Datos de entrada invalidos\"}")))
    })
    @GetMapping("/consolidado")
    public ConsolidadoResponse consolidado(
            @Parameter(description = "Identificador del afiliado", example = "AF-001", required = true)
            @RequestParam String afiliadoId,
            @Parameter(description = "Periodo inicial en formato YYYY-MM", example = "2026-01", required = true)
            @RequestParam String periodoDesde,
            @Parameter(description = "Periodo final en formato YYYY-MM", example = "2026-12", required = true)
            @RequestParam String periodoHasta) {
        var query = new ConsultarAportesQuery(afiliadoId, periodoDesde, periodoHasta);
        return ConsolidadoResponse.from(consultarAportesUseCase.consultar(query));
    }
}
