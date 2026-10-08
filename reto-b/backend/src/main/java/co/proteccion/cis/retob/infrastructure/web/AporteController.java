package co.proteccion.cis.retob.infrastructure.web;

import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase;
import co.proteccion.cis.retob.domain.port.in.ConsultarAportesUseCase.ConsultarAportesQuery;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase.Creado;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase.RegistrarAporteCommand;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase.Repetido;
import co.proteccion.cis.retob.infrastructure.web.dto.AporteResponse;
import co.proteccion.cis.retob.infrastructure.web.dto.ConsolidadoResponse;
import co.proteccion.cis.retob.infrastructure.web.dto.RegistrarAporteRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/aportes")
@RequiredArgsConstructor
public class AporteController {

    private static final String PERIODO_REGEX = "\\d{4}-(0[1-9]|1[0-2])";

    private final RegistrarAporteUseCase registrarAporteUseCase;
    private final ConsultarAportesUseCase consultarAportesUseCase;

    /**
     * Registra un aporte. Responde 201 si se creó y 200 si es un reintento idempotente
     * (misma clave y mismo contenido), devolviendo en ambos casos el aporte.
     */
    @PostMapping
    public ResponseEntity<AporteResponse> registrar(@Valid @RequestBody RegistrarAporteRequest req) {
        var command = new RegistrarAporteCommand(
                req.afiliadoId(),
                req.monto(),
                req.fecha(),
                req.canal(),
                req.idempotenciaKey()
        );
        return switch (registrarAporteUseCase.registrar(command)) {
            case Creado creado -> ResponseEntity.status(HttpStatus.CREATED).body(AporteResponse.from(creado.aporte()));
            case Repetido repetido -> ResponseEntity.ok(AporteResponse.from(repetido.aporte()));
        };
    }

    @GetMapping("/consolidado")
    public ConsolidadoResponse consolidado(
            @RequestParam
            @NotBlank(message = "El afiliadoId es obligatorio")
            String afiliadoId,
            @RequestParam
            @Pattern(regexp = PERIODO_REGEX, message = "El periodoDesde debe tener formato YYYY-MM")
            String periodoDesde,
            @RequestParam
            @Pattern(regexp = PERIODO_REGEX, message = "El periodoHasta debe tener formato YYYY-MM")
            String periodoHasta) {
        var query = new ConsultarAportesQuery(afiliadoId, periodoDesde, periodoHasta);
        return ConsolidadoResponse.from(consultarAportesUseCase.consultar(query));
    }
}
