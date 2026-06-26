package co.proteccion.cis.retoa.controller;

import co.proteccion.cis.retoa.domain.Aporte;
import co.proteccion.cis.retoa.dto.AporteRequest;
import co.proteccion.cis.retoa.service.AporteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/aportes")
@RequiredArgsConstructor
public class AporteController {

    private final AporteService service;

    @PostMapping
    public Aporte registrar(@Valid @RequestBody AporteRequest req) {
        return service.registrar(req);
    }

    @GetMapping("/consolidado")
    public List<Aporte> consolidado(@RequestParam String afiliadoId,
                                    @RequestParam String periodo) {
        return service.obtenerConsolidado(afiliadoId, periodo);
    }
}
