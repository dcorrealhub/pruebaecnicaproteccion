package co.proteccion.cis.retob.domain.port.out;

import co.proteccion.cis.retob.domain.model.Aporte;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida: abstracción de persistencia para aportes.
 * La implementación vive en la capa de infraestructura.
 */
public interface AporteRepositoryPort {

    /**
     * Persiste el aporte. Si otra transacción ya registró la misma clave de
     * idempotencia, lanza {@link co.proteccion.cis.retob.domain.exception.ConflictoConcurrenciaException}.
     */
    Aporte guardar(Aporte aporte);

    Optional<Aporte> findByIdempotenciaKey(String idempotenciaKey);

    /** Aportes del afiliado con periodo en [desde, hasta], ordenados por fecha y luego por id. */
    List<Aporte> findByAfiliadoIdAndPeriodoBetween(String afiliadoId,
                                                    String periodoDesde,
                                                    String periodoHasta);
}
