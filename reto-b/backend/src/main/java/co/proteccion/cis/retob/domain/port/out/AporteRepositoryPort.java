package co.proteccion.cis.retob.domain.port.out;

import co.proteccion.cis.retob.domain.model.Aporte;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida: abstracción de persistencia para aportes.
 * La implementación vive en la capa de infraestructura.
 */
public interface AporteRepositoryPort {

    Aporte guardar(Aporte aporte);

    Optional<Aporte> findByIdempotenciaKey(String idempotenciaKey);

    List<Aporte> findByAfiliadoIdAndPeriodoBetween(String afiliadoId,
                                                    String periodoDesde,
                                                    String periodoHasta);
}
