package co.proteccion.cis.retob.infrastructure.web;

import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase;
import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase.ConsultarAportesQuery;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase.RegistrarAporteCommand;
import co.proteccion.cis.retob.infrastructure.web.dto.AporteResponse;
import co.proteccion.cis.retob.infrastructure.web.dto.ConsolidadoResponse;
import co.proteccion.cis.retob.infrastructure.web.dto.ConsultarConsolidadoRequest;
import co.proteccion.cis.retob.infrastructure.web.dto.RegistrarAporteRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/aportes")
@Validated
@RequiredArgsConstructor
public class AporteController {

    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final RegistrarAporteUseCase registrarAporteUseCase;
    private final ConsultarAportesUseCase consultarAportesUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AporteResponse registrar(
            @Valid @RequestBody RegistrarAporteRequest req,
            @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKeyHeader) {
        String idempotenciaKey = resolverIdempotenciaKey(idempotencyKeyHeader, req.idempotenciaKey());
        var command = new RegistrarAporteCommand(
                req.afiliadoId(),
                req.monto(),
                req.canal(),
                idempotenciaKey
        );
        return AporteResponse.from(registrarAporteUseCase.registrar(command));
    }

    @GetMapping("/consolidado")
    public ConsolidadoResponse consolidado(@Valid @ModelAttribute ConsultarConsolidadoRequest request) {
        var query = new ConsultarAportesQuery(
                request.afiliadoId(),
                request.periodoDesde(),
                request.periodoHasta()
        );
        return ConsolidadoResponse.from(consultarAportesUseCase.consultar(query));
    }

    private String resolverIdempotenciaKey(String header, String body) {
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        if (body != null && !body.isBlank()) {
            return body.trim();
        }
        throw new IllegalArgumentException(
                "Se requiere clave de idempotencia en header '" + IDEMPOTENCY_HEADER + "' o en el body (idempotenciaKey)");
    }
}
