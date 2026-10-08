package co.proteccion.cis.retob.infrastructure.web;

import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase;
import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase.ConsultarAportesQuery;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase.RegistrarAporteCommand;
import co.proteccion.cis.retob.infrastructure.web.dto.AporteResponse;
import co.proteccion.cis.retob.infrastructure.web.dto.ConsolidadoResponse;
import co.proteccion.cis.retob.infrastructure.web.dto.RegistrarAporteRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/aportes")
@RequiredArgsConstructor
public class AporteController {

    private final RegistrarAporteUseCase registrarAporteUseCase;
    private final ConsultarAportesUseCase consultarAportesUseCase;

    /**
     * 201 si el aporte se creó; 200 si la idempotenciaKey ya existía y se devuelve el aporte original.
     */
    @PostMapping
    public ResponseEntity<AporteResponse> registrar(@Valid @RequestBody RegistrarAporteRequest req) {
        var command = new RegistrarAporteCommand(
                req.afiliadoId(),
                req.monto(),
                req.canal(),
                req.idempotenciaKey()
        );
        var aporte = registrarAporteUseCase.registrar(command);
        var status = aporte.isYaExistia() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(AporteResponse.from(aporte));
    }

    /**
     * Consolidado por afiliado. Acepta un único {@code periodo} (yyyy-MM)
     * o un rango {@code periodoDesde}/{@code periodoHasta} (contrato usado por el frontend).
     */
    @GetMapping("/consolidado")
    public ConsolidadoResponse consolidado(
            @RequestParam String afiliadoId,
            @RequestParam(required = false) String periodo,
            @RequestParam(required = false) String periodoDesde,
            @RequestParam(required = false) String periodoHasta) {
        var desde = periodo != null ? periodo : periodoDesde;
        var hasta = periodo != null ? periodo : periodoHasta;
        var query = new ConsultarAportesQuery(afiliadoId, desde, hasta);
        return ConsolidadoResponse.from(consultarAportesUseCase.consultar(query));
    }
}
