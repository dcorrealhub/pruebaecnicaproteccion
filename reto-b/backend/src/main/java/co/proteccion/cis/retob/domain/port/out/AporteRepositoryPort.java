package co.proteccion.cis.retob.domain.port.out;

import co.proteccion.cis.retob.domain.model.Aporte;

import java.util.List;
import java.util.Optional;

public interface AporteRepositoryPort {

    Aporte guardar(Aporte aporte);

    Optional<Aporte> findById(String id);

    Optional<Aporte> findByIdempotenciaKey(String idempotenciaKey);

    List<Aporte> findByAfiliadoIdAndPeriodoBetween(String afiliadoId,
                                                    String periodoDesde,
                                                    String periodoHasta);
}
