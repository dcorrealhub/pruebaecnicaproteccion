package co.proteccion.cis.retob.domain.model.aporte.gateway;

import co.proteccion.cis.retob.domain.model.aporte.Aporte;

import java.util.List;
import java.util.Optional;

public interface AporteOutputPort {

    Aporte save(Aporte aporte);

    Optional<Aporte> findById(Long id);

    Optional<Aporte> findByIdempotenciaKey(String idempotenciaKey);

    List<Aporte> findByAfiliadoIdAndPeriodoBetween(String afiliadoId, String periodoDesde, String periodoHasta);
}
