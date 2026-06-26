package co.proteccion.cis.retob.infrastructure.web;

import co.proteccion.cis.retob.domain.port.in.ActualizarParametrosUseCase;
import co.proteccion.cis.retob.domain.port.in.ActualizarParametrosUseCase.ActualizarParametrosCommand;
import co.proteccion.cis.retob.domain.port.in.ConsultarParametrosUseCase;
import co.proteccion.cis.retob.infrastructure.web.dto.ActualizarParametrosRequest;
import co.proteccion.cis.retob.infrastructure.web.dto.ParametrosResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parametros")
@RequiredArgsConstructor
@Tag(name = "Parámetros del fondo", description = "Gestión de topes y umbrales con historial de cambios auditado")
public class ParametrosController {

    private final ConsultarParametrosUseCase consultarParametrosUseCase;
    private final ActualizarParametrosUseCase actualizarParametrosUseCase;

    @Operation(
            summary = "Parámetros vigentes",
            description = """
                    Retorna el registro más reciente de `historico_parametros`.
                    Al primer arranque del sistema, se siembra automáticamente desde las
                    variables de entorno `APORTE_TOPE_MENSUAL` y `APORTE_UMBRAL_REVISION`.
                    Retorna **204 No Content** si la tabla está vacía.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Parámetros activos"),
            @ApiResponse(responseCode = "204", description = "Tabla de parámetros vacía")
    })
    @GetMapping("/actual")
    public ResponseEntity<ParametrosResponse> consultarActual() {
        return consultarParametrosUseCase.consultarActual()
                .map(p -> ResponseEntity.ok(ParametrosResponse.from(p)))
                .orElse(ResponseEntity.noContent().build());
    }

    @Operation(
            summary = "Historial completo de cambios en parámetros",
            description = "Retorna todos los registros de `historico_parametros` ordenados del más antiguo al más reciente, permitiendo trazar quién modificó los topes y cuándo."
    )
    @ApiResponse(responseCode = "200", description = "Historial de cambios")
    @GetMapping("/historial")
    public List<ParametrosResponse> consultarHistorial() {
        return consultarParametrosUseCase.consultarHistorial()
                .stream()
                .map(ParametrosResponse::from)
                .toList();
    }

    @Operation(
            summary = "Actualizar topes y umbrales",
            description = """
                    Inserta un nuevo registro en `historico_parametros`. El valor activo
                    pasa a ser este nuevo registro de forma inmediata — los siguientes aportes
                    usarán los nuevos topes sin necesidad de reiniciar el servicio.

                    **Invariante requerida:** `montoMinimo` < `umbralRevision` < `topeMensual`
                    - `montoMinimo`: valor mínimo que debe tener un aporte para ser aceptado
                    - `umbralRevision`: aportes que superen este valor quedan en `EN_REVISION`
                    - `topeMensual`: suma máxima que un afiliado puede aportar en un mes
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Parámetros actualizados"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(example = "{\"error\":\"VALIDACION_FALLIDA\",\"mensaje\":\"...\"}")))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParametrosResponse actualizar(@Valid @RequestBody ActualizarParametrosRequest req) {
        var command = new ActualizarParametrosCommand(
                req.montoMinimo(),
                req.topeMensual(),
                req.umbralRevision(),
                req.modificadoPor(),
                req.comentario()
        );
        return ParametrosResponse.from(actualizarParametrosUseCase.actualizar(command));
    }
}
