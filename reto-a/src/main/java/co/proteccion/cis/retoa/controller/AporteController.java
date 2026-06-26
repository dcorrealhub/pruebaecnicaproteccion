package co.proteccion.cis.retoa.controller;

import co.proteccion.cis.retoa.domain.Aporte;
import co.proteccion.cis.retoa.dto.AporteRequest;
import co.proteccion.cis.retoa.service.AporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Aportes", description = "Registro y consulta de aportes voluntarios al fondo de pensiones")
@RestController
@RequestMapping("/api/aportes")
@RequiredArgsConstructor
public class AporteController {

    private final AporteService service;

    @Operation(
        summary = "Registrar aporte voluntario",
        description = """
            Registra un aporte voluntario al fondo de pensiones. La operación es idempotente:
            si se envía la misma `idempotencyKey`, se retorna el aporte original sin crear un duplicado.
            El aporte se marcará para revisión de cumplimiento si supera el umbral configurado (por defecto 5.000.000 COP).
            """)
    @ApiResponse(responseCode = "201", description = "Aporte registrado correctamente",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = Aporte.class)))
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos (monto negativo, canal no válido, UUID malformado)",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ApiResponse(responseCode = "404", description = "Afiliado no encontrado",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ApiResponse(responseCode = "409", description = "Aporte duplicado — llave de idempotencia ya utilizada en otro afiliado",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ApiResponse(responseCode = "422", description = "El monto supera el tope mensual permitido (por defecto 10.000.000 COP)",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Aporte registrar(@Valid @RequestBody AporteRequest req) {
        return service.registrar(req);
    }

    @Operation(
        summary = "Consultar aportes del periodo",
        description = "Retorna todos los aportes registrados para un afiliado en el periodo indicado (formato `YYYY-MM`).")
    @ApiResponse(responseCode = "200", description = "Lista de aportes (puede ser vacía)",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = Aporte.class)))
    @GetMapping("/consolidado")
    public List<Aporte> consolidado(
            @Parameter(description = "Identificador del afiliado", example = "AF-001", required = true)
            @RequestParam String afiliadoId,
            @Parameter(description = "Periodo en formato YYYY-MM", example = "2026-06", required = true)
            @RequestParam String periodo) {
        return service.consolidado(afiliadoId, periodo);
    }
}
