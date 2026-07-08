package co.proteccion.cis.retob.infrastructure.adapter.rest;

import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase;
import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.ConsolidadoAportes;
import co.proteccion.cis.retob.infrastructure.adapter.rest.dto.AporteRequest;
import co.proteccion.cis.retob.infrastructure.adapter.rest.dto.ConsolidadoRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/aportes")
public class AporteController {

    private final RegistrarAporteUseCase registrarAporteUseCase;
    private final ConsultarAportesUseCase consultarAportesUseCase;

    public AporteController(RegistrarAporteUseCase registrarAporteUseCase, ConsultarAportesUseCase consultarAportesUseCase) {
        this.registrarAporteUseCase = registrarAporteUseCase;
        this.consultarAportesUseCase = consultarAportesUseCase;
    }

    @PostMapping
    public ResponseEntity<Aporte> registrar(@Valid @RequestBody AporteRequest request) {
        RegistrarAporteUseCase.RegistrarAporteCommand command = new RegistrarAporteUseCase.RegistrarAporteCommand(
                request.idAfiliado(),
                request.monto(),
                request.canal(),
                request.idempotenciaKey()
        );
        Aporte aporte = registrarAporteUseCase.registrar(command);
        return ResponseEntity.created(URI.create("/api/aportes/" + aporte.getId())).body(aporte);
    }

    @GetMapping("/consolidado")
    public ResponseEntity<ConsolidadoAportes> consolidado(@Valid ConsolidadoRequest request) {
        ConsultarAportesUseCase.ConsultarAportesQuery query = new ConsultarAportesUseCase.ConsultarAportesQuery(
                request.idAfiliado(),
                request.periodoDesde(),
                request.periodoHasta()
        );
        ConsolidadoAportes consolidado = consultarAportesUseCase.consultar(query);
        return ResponseEntity.ok(consolidado);
    }
}