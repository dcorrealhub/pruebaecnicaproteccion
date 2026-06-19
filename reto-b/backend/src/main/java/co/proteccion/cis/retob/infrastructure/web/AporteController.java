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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/aportes")
@RequiredArgsConstructor
public class AporteController {

    private final RegistrarAporteUseCase registrarAporteUseCase;
    private final ConsultarAportesUseCase consultarAportesUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AporteResponse registrar(
            @RequestHeader("Idempotency-Key") String idempotenciaKey,
            @Valid @RequestBody RegistrarAporteRequest req) {
        log.info("Registrar aporte — afiliadoId={}, canal={}", req.afiliadoId(), req.canal());
        var command = new RegistrarAporteCommand(
                req.afiliadoId(),
                req.monto(),
                req.fecha(),
                req.canal(),
                idempotenciaKey
        );
        AporteResponse response = AporteResponse.from(registrarAporteUseCase.registrar(command));
        log.info("Aporte registrado — id={}, marcadaRevision={}", response.id(), response.marcadaRevision());
        return response;
    }

    @GetMapping("/{afiliadoId}/consolidado")
    public ConsolidadoResponse consolidado(
            @PathVariable String afiliadoId,
            @RequestParam String periodoDesde,
            @RequestParam String periodoHasta) {
        log.info("Consultar consolidado — afiliadoId={}, desde={}, hasta={}", afiliadoId, periodoDesde, periodoHasta);
        var query = new ConsultarAportesQuery(afiliadoId, periodoDesde, periodoHasta);
        return ConsolidadoResponse.from(consultarAportesUseCase.consultar(query));
    }
}
