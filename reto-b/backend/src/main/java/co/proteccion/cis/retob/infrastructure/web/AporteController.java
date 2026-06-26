package co.proteccion.cis.retob.infrastructure.web;

import co.proteccion.cis.retob.domain.port.in.CambiarEstadoAporteUseCase;
import co.proteccion.cis.retob.domain.port.in.CambiarEstadoAporteUseCase.CambiarEstadoCommand;
import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase;
import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase.ConsultarAportesQuery;
import co.proteccion.cis.retob.domain.port.in.ConsultarRevisionesUseCase;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase.RegistrarAporteCommand;
import co.proteccion.cis.retob.infrastructure.web.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/aportes")
@RequiredArgsConstructor
public class AporteController {

    private final RegistrarAporteUseCase registrarAporteUseCase;
    private final ConsultarAportesUseCase consultarAportesUseCase;
    private final CambiarEstadoAporteUseCase cambiarEstadoAporteUseCase;
    private final ConsultarRevisionesUseCase consultarRevisionesUseCase;

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

    @GetMapping("/consolidado")
    public ConsolidadoResponse consolidado(
            @RequestParam String afiliadoId,
            @RequestParam String periodoDesde,
            @RequestParam String periodoHasta) {
        var query = new ConsultarAportesQuery(afiliadoId, periodoDesde, periodoHasta);
        return ConsolidadoResponse.from(consultarAportesUseCase.consultar(query));
    }

    @PatchMapping("/{id}/estado")
    public AporteResponse cambiarEstado(@PathVariable Long id,
                                         @Valid @RequestBody CambiarEstadoRequest req) {
        var command = new CambiarEstadoCommand(id, req.nuevoEstado(), req.revisor(), req.comentario());
        return AporteResponse.from(cambiarEstadoAporteUseCase.cambiar(command));
    }

    @GetMapping("/{id}/revisiones")
    public List<RevisionResponse> revisiones(@PathVariable Long id) {
        return consultarRevisionesUseCase.consultar(id)
                .stream()
                .map(RevisionResponse::from)
                .toList();
    }
}
