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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/aportes")
@RequiredArgsConstructor
public class AporteController {

    private static final String PATRON_AFILIADO = "^[A-Za-z0-9-]{1,50}$";
    private static final String PATRON_PERIODO = "^\\d{4}-(0[1-9]|1[0-2])$";

    private final RegistrarAporteUseCase registrarAporteUseCase;
    private final ConsultarAportesUseCase consultarAportesUseCase;

    /**
     * 201 cuando el aporte se crea; 200 cuando es un reintento idempotente
     * (mismo cuerpo, misma clave) y se devuelve el aporte original.
     */
    @PostMapping
    public ResponseEntity<AporteResponse> registrar(@Valid @RequestBody RegistrarAporteRequest req) {
        var command = new RegistrarAporteCommand(
                req.afiliadoId(),
                req.monto(),
                req.canal(),
                req.idempotenciaKey()
        );
        var resultado = registrarAporteUseCase.registrar(command);
        return ResponseEntity
                .status(resultado.creado() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(AporteResponse.from(resultado.aporte()));
    }

    @GetMapping("/consolidado")
    public ConsolidadoResponse consolidado(
            @RequestParam @Pattern(regexp = PATRON_AFILIADO, message = "afiliadoId inválido") String afiliadoId,
            @RequestParam @Pattern(regexp = PATRON_PERIODO, message = "periodoDesde debe tener formato YYYY-MM") String periodoDesde,
            @RequestParam @Pattern(regexp = PATRON_PERIODO, message = "periodoHasta debe tener formato YYYY-MM") String periodoHasta) {
        var query = new ConsultarAportesQuery(afiliadoId, periodoDesde, periodoHasta);
        return ConsolidadoResponse.from(consultarAportesUseCase.consultar(query));
    }
}
