package co.proteccion.cis.retob.infrastructure.web;

import co.proteccion.cis.retob.domain.port.in.ConfigurarParametrosUseCase;
import co.proteccion.cis.retob.infrastructure.web.dto.ParametrosRequest;
import co.proteccion.cis.retob.infrastructure.web.dto.ParametrosResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Configuración de los parámetros globales (tope mensual y umbral de revisión).
 * Los cambios aplican en runtime: el registro de aportes lee la tabla en cada operación.
 */
@RestController
@RequestMapping("/api/configuracion/parametros")
@RequiredArgsConstructor
public class ConfiguracionController {

    private final ConfigurarParametrosUseCase configurarParametrosUseCase;

    @GetMapping
    public ParametrosResponse obtener() {
        return ParametrosResponse.from(configurarParametrosUseCase.obtenerGlobal());
    }

    @PutMapping
    public ParametrosResponse actualizar(@Valid @RequestBody ParametrosRequest req) {
        return ParametrosResponse.from(
                configurarParametrosUseCase.actualizarGlobal(req.topeMensual(), req.umbralRevision()));
    }
}
