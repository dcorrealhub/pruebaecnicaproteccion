package co.proteccion.cis.retob.infrastructure.web;

import co.proteccion.cis.retob.domain.port.in.CambiarEstadoAfiliadoUseCase;
import co.proteccion.cis.retob.domain.port.in.CambiarEstadoAfiliadoUseCase.CambiarEstadoAfiliadoCommand;
import co.proteccion.cis.retob.domain.port.in.ConsultarAfiliadoUseCase;
import co.proteccion.cis.retob.domain.port.in.RegistrarAfiliadoUseCase;
import co.proteccion.cis.retob.domain.port.in.RegistrarAfiliadoUseCase.RegistrarAfiliadoCommand;
import co.proteccion.cis.retob.domain.port.out.HistorialEstadoAfiliadoRepository;
import co.proteccion.cis.retob.infrastructure.web.dto.AfiliadoResponse;
import co.proteccion.cis.retob.infrastructure.web.dto.CambiarEstadoAfiliadoRequest;
import co.proteccion.cis.retob.infrastructure.web.dto.HistorialEstadoAfiliadoResponse;
import co.proteccion.cis.retob.infrastructure.web.dto.RegistrarAfiliadoRequest;
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

import java.util.List;

@RestController
@RequestMapping("/api/afiliados")
@RequiredArgsConstructor
@Tag(name = "Afiliados", description = "Catálogo y gestión del ciclo de vida de afiliados al fondo voluntario")
public class AfiliadoController {

    private final RegistrarAfiliadoUseCase       registrarAfiliadoUseCase;
    private final ConsultarAfiliadoUseCase       consultarAfiliadoUseCase;
    private final CambiarEstadoAfiliadoUseCase   cambiarEstadoAfiliadoUseCase;
    private final HistorialEstadoAfiliadoRepository historialRepository;

    @Operation(
            summary = "Registrar un afiliado",
            description = "Crea un nuevo afiliado con estado `ACTIVO`. El `afiliadoId` debe ser único en el sistema."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Afiliado creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(example = "{\"error\":\"VALIDACION_FALLIDA\",\"mensaje\":\"...\"}"))),
            @ApiResponse(responseCode = "409", description = "Ya existe un afiliado con ese afiliadoId",
                    content = @Content(schema = @Schema(example = "{\"error\":\"CONFLICTO\",\"mensaje\":\"...\"}")))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AfiliadoResponse registrar(@Valid @RequestBody RegistrarAfiliadoRequest req) {
        var command = new RegistrarAfiliadoCommand(req.afiliadoId(), req.nombre());
        return AfiliadoResponse.from(registrarAfiliadoUseCase.registrar(command));
    }

    @Operation(summary = "Listar todos los afiliados")
    @ApiResponse(responseCode = "200", description = "Lista de afiliados")
    @GetMapping
    public List<AfiliadoResponse> consultarTodos() {
        return consultarAfiliadoUseCase.consultarTodos()
                .stream()
                .map(AfiliadoResponse::from)
                .toList();
    }

    @Operation(summary = "Consultar un afiliado por su ID sintético")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Afiliado encontrado"),
            @ApiResponse(responseCode = "404", description = "Afiliado no encontrado")
    })
    @GetMapping("/{afiliadoId}")
    public AfiliadoResponse consultar(
            @Parameter(description = "ID sintético del afiliado", example = "AF-001")
            @PathVariable String afiliadoId) {
        return AfiliadoResponse.from(consultarAfiliadoUseCase.consultar(afiliadoId));
    }

    @Operation(
            summary = "Cambiar estado del afiliado (bloquear / desbloquear)",
            description = """
                    Permite a un operador cambiar el estado del afiliado entre `ACTIVO` y `BLOQUEADO`.

                    - **BLOQUEADO**: el afiliado no podrá registrar nuevos aportes. Sus aportes existentes
                      no se ven afectados.
                    - **ACTIVO**: reactiva al afiliado, habilitando nuevos aportes.

                    El cambio queda registrado automáticamente en `historial_estado_afiliado`
                    via trigger de base de datos.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "El afiliado ya está en el estado solicitado"),
            @ApiResponse(responseCode = "404", description = "Afiliado no encontrado")
    })
    @PatchMapping("/{afiliadoId}/estado")
    public AfiliadoResponse cambiarEstado(
            @Parameter(description = "ID sintético del afiliado", example = "AF-001")
            @PathVariable String afiliadoId,
            @Valid @RequestBody CambiarEstadoAfiliadoRequest req) {
        return AfiliadoResponse.from(cambiarEstadoAfiliadoUseCase.cambiar(
                new CambiarEstadoAfiliadoCommand(afiliadoId, req.nuevoEstado())));
    }

    @Operation(
            summary = "Historial de cambios de estado del afiliado",
            description = """
                    Retorna el registro auditado de todos los cambios de estado del afiliado,
                    ordenados del más reciente al más antiguo.

                    Este historial es poblado automáticamente por un trigger de base de datos
                    (`trg_afiliado_historial_estado`) cada vez que el estado cambia.
                    Proporciona trazabilidad completa para auditorías de compliance.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial retornado (puede ser vacío)"),
            @ApiResponse(responseCode = "404", description = "Afiliado no encontrado")
    })
    @GetMapping("/{afiliadoId}/historial-estado")
    public List<HistorialEstadoAfiliadoResponse> historialEstado(
            @Parameter(description = "ID sintético del afiliado", example = "AF-001")
            @PathVariable String afiliadoId) {
        consultarAfiliadoUseCase.consultar(afiliadoId); // valida existencia
        return historialRepository.findByAfiliadoId(afiliadoId)
                .stream()
                .map(HistorialEstadoAfiliadoResponse::from)
                .toList();
    }
}
