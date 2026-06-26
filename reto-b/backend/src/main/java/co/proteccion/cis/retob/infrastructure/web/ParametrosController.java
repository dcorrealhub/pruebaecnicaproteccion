package co.proteccion.cis.retob.infrastructure.web;

import co.proteccion.cis.retob.domain.port.in.ActualizarParametrosUseCase;
import co.proteccion.cis.retob.domain.port.in.ActualizarParametrosUseCase.ActualizarParametrosCommand;
import co.proteccion.cis.retob.domain.port.in.ConsultarParametrosUseCase;
import co.proteccion.cis.retob.infrastructure.web.dto.ActualizarParametrosRequest;
import co.proteccion.cis.retob.infrastructure.web.dto.ParametrosResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parametros")
@RequiredArgsConstructor
public class ParametrosController {

    private final ConsultarParametrosUseCase consultarParametrosUseCase;
    private final ActualizarParametrosUseCase actualizarParametrosUseCase;

    @GetMapping("/actual")
    public ResponseEntity<ParametrosResponse> consultarActual() {
        return consultarParametrosUseCase.consultarActual()
                .map(p -> ResponseEntity.ok(ParametrosResponse.from(p)))
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/historial")
    public List<ParametrosResponse> consultarHistorial() {
        return consultarParametrosUseCase.consultarHistorial()
                .stream()
                .map(ParametrosResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParametrosResponse actualizar(@Valid @RequestBody ActualizarParametrosRequest req) {
        var command = new ActualizarParametrosCommand(
                req.topeMensual(),
                req.umbralRevision(),
                req.modificadoPor(),
                req.comentario()
        );
        return ParametrosResponse.from(actualizarParametrosUseCase.actualizar(command));
    }
}
