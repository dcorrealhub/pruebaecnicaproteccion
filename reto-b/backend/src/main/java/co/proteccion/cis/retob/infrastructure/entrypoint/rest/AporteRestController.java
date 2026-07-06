package co.proteccion.cis.retob.infrastructure.entrypoint.rest;

import co.proteccion.cis.retob.application.handler.AporteHandler;
import co.proteccion.cis.retob.domain.model.aporte.ConsultaConsolidado;
import co.proteccion.cis.retob.infrastructure.entrypoint.dto.request.RegistrarAporteRequest;
import co.proteccion.cis.retob.infrastructure.entrypoint.dto.response.AporteResponse;
import co.proteccion.cis.retob.infrastructure.entrypoint.dto.response.ConsolidadoResponse;
import co.proteccion.cis.retob.infrastructure.entrypoint.mapper.AporteRestMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/aportes")
@RequiredArgsConstructor
public class AporteRestController {

    private final AporteHandler aporteHandler;
    private final AporteRestMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AporteResponse registrar(@Valid @RequestBody RegistrarAporteRequest request) {
        return mapper.toResponse(aporteHandler.registrar(mapper.toDomain(request)));
    }

    @GetMapping("/consolidado")
    public ConsolidadoResponse consolidado(@RequestParam String afiliadoId,
                                           @RequestParam String periodoDesde,
                                           @RequestParam String periodoHasta) {
        return mapper.toResponse(
                aporteHandler.consultar(new ConsultaConsolidado(afiliadoId, periodoDesde, periodoHasta)));
    }

    @PostMapping("/{id}/aprobar")
    public AporteResponse aprobar(@PathVariable Long id) {
        return mapper.toResponse(aporteHandler.aprobar(id));
    }

    @PostMapping("/{id}/rechazar")
    public AporteResponse rechazar(@PathVariable Long id) {
        return mapper.toResponse(aporteHandler.rechazar(id));
    }
}
