package co.proteccion.cis.retob.domain.port.out;

import co.proteccion.cis.retob.domain.model.Aporte;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida: abstracción de persistencia para aportes.
 * La implementación vive en la capa de infraestructura.
 */
public interface AporteRepositoryPort {

    Aporte guardar(Aporte aporte);

    Optional<Aporte> findById(Long id);

    Optional<Aporte> findByIdempotenciaKey(String idempotenciaKey);

    List<Aporte> findByAfiliadoIdAndPeriodoBetween(String afiliadoId,
                                                    String periodoDesde,
                                                    String periodoHasta);

    /**
     * Consulta optimizada: total agregado en BD + detalle filtrado por afiliado y rango de periodos.
     */
    ConsolidadoConsulta buscarConsolidado(String afiliadoId,
                                          String periodoDesde,
                                          String periodoHasta);

    record ConsolidadoConsulta(BigDecimal totalAportado, List<Aporte> aportes) {}
}
