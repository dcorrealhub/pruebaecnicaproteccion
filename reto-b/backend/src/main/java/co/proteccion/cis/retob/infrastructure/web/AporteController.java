package co.proteccion.cis.retob.infrastructure.web;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase;
import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase.ConsultarAportesQuery;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase.RegistrarAporteCommand;
import co.proteccion.cis.retob.infrastructure.web.dto.AporteResponse;
import co.proteccion.cis.retob.infrastructure.web.dto.ConsolidadoResponse;
import co.proteccion.cis.retob.infrastructure.web.dto.RegistrarAporteRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/aportes")
public class AporteController {

    private final RegistrarAporteUseCase registrarAporteUseCase;
    private final ConsultarAportesUseCase consultarAportesUseCase;

    public AporteController(RegistrarAporteUseCase registrarAporteUseCase,
                            ConsultarAportesUseCase consultarAportesUseCase) {
        this.registrarAporteUseCase = registrarAporteUseCase;
        this.consultarAportesUseCase = consultarAportesUseCase;
    }

    @PostMapping
    public ResponseEntity<AporteResponse> registrar(@Valid @RequestBody RegistrarAporteRequest req,
                                                    UriComponentsBuilder uriBuilder) {
        var command = new RegistrarAporteCommand(
                req.afiliadoId(),
                req.monto(),
                req.fecha(),
                req.canal(),
                req.idempotenciaKey()
        );
        Aporte aporte = registrarAporteUseCase.registrar(command);

        URI location = uriBuilder.path("/api/aportes/{id}").buildAndExpand(aporte.getId()).toUri();
        return ResponseEntity.created(location).body(AporteResponse.from(aporte));
    }

    @GetMapping("/{id}")
    public AporteResponse porId(@PathVariable Long id) {
        return AporteResponse.from(consultarAportesUseCase.buscarPorId(id));
    }

    @GetMapping("/consolidado")
    public ConsolidadoResponse consolidado(
            @RequestParam String afiliadoId,
            @RequestParam String periodoDesde,
            @RequestParam String periodoHasta) {
        var query = new ConsultarAportesQuery(afiliadoId, periodoDesde, periodoHasta);
        return ConsolidadoResponse.from(consultarAportesUseCase.consultar(query));
    }
}
