package co.proteccion.cis.retob.infrastructure.persistence.adapter;

import co.proteccion.cis.retob.domain.exception.ConcurrenciaConflictoException;
import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.infrastructure.persistence.mappers.AporteMapper;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataAporteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador JPA para el puerto de salida {@link AporteRepositoryPort}.
 *
 * <p>Si otra transacción concurrente registró un aporte con la misma clave de idempotencia,
 * la restricción única se traduce a {@link ConcurrenciaConflictoException}: al reintentar,
 * el caso de uso encontrará el aporte existente.
 */
@Repository
@RequiredArgsConstructor
public class JpaAporteRepositoryAdapter implements AporteRepositoryPort {

    private static final String UQ_IDEMPOTENCIA = "uq_aporte_idempotencia";

    private final SpringDataAporteRepository springDataRepo;

    @Override
    public Aporte guardar(Aporte aporte) {
        try {
            return AporteMapper.toDomain(springDataRepo.saveAndFlush(AporteMapper.toEntity(aporte)));
        } catch (DataIntegrityViolationException e) {
            if (ViolacionRestriccion.es(e, UQ_IDEMPOTENCIA)) {
                throw new ConcurrenciaConflictoException(
                        "Otro registro concurrente usó la misma clave de idempotencia; reintente la operación", e);
            }
            throw e;
        }
    }

    @Override
    public Optional<Aporte> findByIdempotenciaKey(String idempotenciaKey) {
        return springDataRepo.findByIdempotenciaKey(idempotenciaKey).map(AporteMapper::toDomain);
    }

    @Override
    public List<Aporte> findByAfiliadoIdAndPeriodoBetween(String afiliadoId,
                                                           String periodoDesde,
                                                           String periodoHasta) {
        return springDataRepo
                .findByAfiliadoIdAndPeriodoBetweenOrderByFechaAscIdAsc(afiliadoId, periodoDesde, periodoHasta)
                .stream()
                .map(AporteMapper::toDomain)
                .toList();
    }
}
