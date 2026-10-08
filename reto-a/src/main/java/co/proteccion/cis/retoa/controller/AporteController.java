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
    // H-007: el controller depende de JdbcTemplate (infraestructura) y accede a la BD sin pasar por servicio/repositorio.
    private final JdbcTemplate jdbc;

    // H-007: mapeo manual de filas en la capa web; duplica el mapeo JPA y acopla el controller al esquema.
    private final RowMapper<Aporte> aporteRowMapper = (rs, rowNum) -> {
        Aporte a = new Aporte();
        a.setId(rs.getLong("id"));
        a.setAfiliadoId(rs.getString("afiliado_id"));
        // H-005: monto leído como double; pierde precisión y admite Infinity.
        a.setMonto(rs.getDouble("monto"));
        // H-007: NPE si fecha es NULL (la BD lo permite, ver H-015).
        a.setFecha(rs.getDate("fecha").toLocalDate());
        a.setCanal(rs.getString("canal"));
        a.setPeriodo(rs.getString("periodo"));
        a.setMarcadaRevision(rs.getBoolean("marcada_revision"));
        return a;
    };

    // H-008: sin Idempotency-Key; un reintento o doble clic duplica el aporte y el saldo.
    // H-013: responde 200 en vez de 201 + Location; errores de negocio terminan en 500.
    // H-009: sin autenticación/autorización; cualquiera registra aportes a nombre de cualquier afiliado.
    @PostMapping
    // H-012: falta @Valid; el request no se valida (afiliadoId nulo, canal libre, monto ausente = 0).
    // H-014: devuelve la entidad JPA como contrato HTTP (expone id interno y marcadaRevision).
    public Aporte registrar(@RequestBody AporteRequest req) {
        return service.registrar(req);
    }

    // H-009: IDOR; se consultan los aportes de cualquier afiliado cambiando el parámetro afiliadoId.
    // H-014: devuelve entidades JPA sin DTO ni paginación.
    @GetMapping("/consolidado")
    public List<Aporte> consolidado(@RequestParam String afiliadoId,
                                    @RequestParam String periodo) {
        // H-001: inyección SQL; parámetros concatenados en la consulta (x' OR '1'='1' -- devuelve todos los afiliados).
        // H-007: SQL ejecutado desde el controller; ya existe AporteJpaRepository.findByAfiliadoIdAndPeriodo parametrizado.
        String sql = "SELECT * FROM aporte WHERE afiliado_id = '"
                + afiliadoId + "' AND periodo = '" + periodo + "'";
        return jdbc.query(sql, aporteRowMapper);
    }
}
