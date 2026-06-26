package co.proteccion.cis.retob.infrastructure.web;

import co.proteccion.cis.retob.domain.port.in.ConsultarAfiliadoUseCase;
import co.proteccion.cis.retob.domain.port.in.RegistrarAfiliadoUseCase;
import co.proteccion.cis.retob.domain.port.in.RegistrarAfiliadoUseCase.RegistrarAfiliadoCommand;
import co.proteccion.cis.retob.infrastructure.web.dto.AfiliadoResponse;
import co.proteccion.cis.retob.infrastructure.web.dto.RegistrarAfiliadoRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/afiliados")
@RequiredArgsConstructor
public class AfiliadoController {

    private final RegistrarAfiliadoUseCase registrarAfiliadoUseCase;
    private final ConsultarAfiliadoUseCase consultarAfiliadoUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AfiliadoResponse registrar(@Valid @RequestBody RegistrarAfiliadoRequest req) {
        var command = new RegistrarAfiliadoCommand(req.afiliadoId(), req.nombre());
        return AfiliadoResponse.from(registrarAfiliadoUseCase.registrar(command));
    }

    @GetMapping("/{afiliadoId}")
    public AfiliadoResponse consultar(@PathVariable String afiliadoId) {
        return AfiliadoResponse.from(consultarAfiliadoUseCase.consultar(afiliadoId));
    }

    @GetMapping
    public List<AfiliadoResponse> consultarTodos() {
        return consultarAfiliadoUseCase.consultarTodos()
                .stream()
                .map(AfiliadoResponse::from)
                .toList();
    }
}
