package co.proteccion.cis.retoa.controller;

import co.proteccion.cis.retoa.domain.Aporte;
import co.proteccion.cis.retoa.dto.AporteRequest;
import co.proteccion.cis.retoa.service.AporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/aportes")
@RequiredArgsConstructor
public class AporteController {

    private final AporteService service;
    private final JdbcTemplate jdbc;

    private final RowMapper<Aporte> aporteRowMapper = (rs, rowNum) -> {
        Aporte a = new Aporte();
        a.setId(rs.getLong("id"));
        a.setAfiliadoId(rs.getString("afiliado_id"));
        a.setMonto(rs.getDouble("monto"));
        a.setFecha(rs.getDate("fecha").toLocalDate());
        a.setCanal(rs.getString("canal"));
        a.setPeriodo(rs.getString("periodo"));
        a.setMarcadaRevision(rs.getBoolean("marcada_revision"));
        return a;
    };

    @PostMapping
    public Aporte registrar(@RequestBody AporteRequest req) {
        return service.registrar(req);
    }

    @GetMapping("/consolidado")
    public List<Aporte> consolidado(@RequestParam String afiliadoId,
                                    @RequestParam String periodo) {
        String sql = "SELECT * FROM aporte WHERE afiliado_id = '"
                + afiliadoId + "' AND periodo = '" + periodo + "'";
        return jdbc.query(sql, aporteRowMapper);
    }
}
