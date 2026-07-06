package co.proteccion.cis.retob.infrastructure.entrypoint.rest;

import co.proteccion.cis.retob.application.handler.ParametroHandler;
import co.proteccion.cis.retob.infrastructure.entrypoint.dto.request.ParametrosRequest;
import co.proteccion.cis.retob.infrastructure.entrypoint.dto.response.ParametrosResponse;
import co.proteccion.cis.retob.infrastructure.entrypoint.mapper.ParametroRestMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/configuracion/parametros")
@RequiredArgsConstructor
public class ConfiguracionRestController {

    private final ParametroHandler parametroHandler;
    private final ParametroRestMapper mapper;

    @GetMapping
    public ParametrosResponse obtener() {
        return mapper.toResponse(parametroHandler.obtenerGlobal());
    }

    @PutMapping
    public ParametrosResponse actualizar(@Valid @RequestBody ParametrosRequest request) {
        return mapper.toResponse(parametroHandler.actualizarGlobal(mapper.toDomain(request)));
    }
}
