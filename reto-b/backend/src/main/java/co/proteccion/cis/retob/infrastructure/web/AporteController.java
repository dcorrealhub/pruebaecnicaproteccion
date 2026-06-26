package co.proteccion.cis.retob.infrastructure.web;

import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase;
import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase.ConsultarAportesQuery;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase.RegistrarAporteCommand;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.infrastructure.web.dto.AporteResponse;
import co.proteccion.cis.retob.infrastructure.web.dto.ConsolidadoResponse;
import co.proteccion.cis.retob.infrastructure.web.dto.RegistrarAporteRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/aportes")
@RequiredArgsConstructor
public class AporteController {

    private final RegistrarAporteUseCase registrarAporteUseCase;
    private final ConsultarAportesUseCase consultarAportesUseCase;
    private final AporteRepositoryPort aporteRepository;

    @PostMapping
    public ResponseEntity<AporteResponse> registrar(@Valid @RequestBody RegistrarAporteRequest req) {
        log.info("POST /api/aportes recibido: afiliadoId={}, monto={}, idempotenciaKey={}",
                req.afiliadoId(), req.monto(), req.idempotenciaKey());
        var command = new RegistrarAporteCommand(
                req.afiliadoId(),
                req.monto(),
                req.canal(),
                req.idempotenciaKey()
        );
        var exists = aporteRepository.findByIdempotenciaKey(req.idempotenciaKey()).isPresent();
        if (exists) {
            log.info("IdempotenciaKey ya existe, retornando 200 OK: key={}", req.idempotenciaKey());
        }
        var response = AporteResponse.from(registrarAporteUseCase.registrar(command));
        var status = exists ? HttpStatus.OK : HttpStatus.CREATED;
        log.info("Respuesta POST: status={}, id={}", status, response.id());
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/consolidado")
    public ConsolidadoResponse consolidado(
            @RequestParam String afiliadoId,
            @RequestParam String periodoDesde,
            @RequestParam String periodoHasta) {
        log.info("GET /api/aportes/consolidado: afiliadoId={}, desde={}, hasta={}",
                afiliadoId, periodoDesde, periodoHasta);
        var query = new ConsultarAportesQuery(afiliadoId, periodoDesde, periodoHasta);
        var result = consultarAportesUseCase.consultar(query);
        log.info("Respuesta GET consolidado: total={}, detalle={} items",
                result.totalAportado(), result.detalle().size());
        return ConsolidadoResponse.from(result);
    }
}
