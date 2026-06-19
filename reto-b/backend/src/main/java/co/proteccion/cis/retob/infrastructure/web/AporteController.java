package co.proteccion.cis.retob.infrastructure.web;

import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase;
import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase.ConsultarAportesQuery;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase.RegistrarAporteCommand;
import co.proteccion.cis.retob.infrastructure.web.dto.AporteResponse;
import co.proteccion.cis.retob.infrastructure.web.dto.ConsolidadoResponse;
import co.proteccion.cis.retob.infrastructure.web.dto.RegistrarAporteRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/aportes")
@RequiredArgsConstructor
@Validated
public class AporteController {

    private final RegistrarAporteUseCase registrarAporteUseCase;
    private final ConsultarAportesUseCase consultarAportesUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AporteResponse registrar(@Valid @RequestBody RegistrarAporteRequest req) {
        var command = new RegistrarAporteCommand(
                req.afiliadoId(),
                req.monto(),
                req.fecha(),
                req.canal(),
                req.idempotenciaKey()
        );
        return AporteResponse.from(registrarAporteUseCase.registrar(command));
    }

    @GetMapping("/consolidado")
    public ConsolidadoResponse consolidado(
            @RequestParam String afiliadoId,
            @RequestParam @Pattern(regexp = "\\d{4}-\\d{2}", message = "El periodoDesde debe tener formato YYYY-MM") String periodoDesde,
            @RequestParam @Pattern(regexp = "\\d{4}-\\d{2}", message = "El periodoHasta debe tener formato YYYY-MM") String periodoHasta) {
        var query = new ConsultarAportesQuery(afiliadoId, periodoDesde, periodoHasta);
        return ConsolidadoResponse.from(consultarAportesUseCase.consultar(query));
    }
}
