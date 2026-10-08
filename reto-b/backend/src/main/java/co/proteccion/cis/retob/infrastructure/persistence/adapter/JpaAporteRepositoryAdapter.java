package co.proteccion.cis.retob.infrastructure.persistence.adapter;

import co.proteccion.cis.retob.domain.exception.ConflictoConcurrenciaException;
import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.Canal;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.infrastructure.persistence.entity.AporteEntity;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataAporteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador JPA para el puerto de salida {@link AporteRepositoryPort}.
 */
@Repository
@RequiredArgsConstructor
public class JpaAporteRepositoryAdapter implements AporteRepositoryPort {

    private final SpringDataAporteRepository springDataRepo;

    @Override
    public Aporte guardar(Aporte aporte) {
        try {
            // saveAndFlush: la violación de unicidad debe aparecer aquí, no en el commit,
            // para poder traducirla a una excepción de dominio.
            return toDomain(springDataRepo.saveAndFlush(toEntity(aporte)));
        } catch (DataIntegrityViolationException e) {
            if (RestriccionesBd.violoRestriccion(e, RestriccionesBd.UQ_APORTE_IDEMPOTENCIA)) {
                throw new ConflictoConcurrenciaException(e);
            }
            throw e;
        }
    }

    @Override
    public Optional<Aporte> findByIdempotenciaKey(String idempotenciaKey) {
        return springDataRepo.findByIdempotenciaKey(idempotenciaKey).map(JpaAporteRepositoryAdapter::toDomain);
    }

    @Override
    public List<Aporte> findByAfiliadoIdAndPeriodoBetween(String afiliadoId,
                                                           String periodoDesde,
                                                           String periodoHasta) {
        return springDataRepo
                .findByAfiliadoIdAndPeriodoBetweenOrderByFechaAscIdAsc(afiliadoId, periodoDesde, periodoHasta)
                .stream()
                .map(JpaAporteRepositoryAdapter::toDomain)
                .toList();
    }

    private static AporteEntity toEntity(Aporte aporte) {
        return AporteEntity.builder()
                .id(aporte.getId())
                .afiliadoId(aporte.getAfiliadoId())
                .monto(aporte.getMonto())
                .fecha(aporte.getFecha())
                .canal(aporte.getCanal().name())
                .periodo(aporte.getPeriodo())
                .marcadaRevision(aporte.isMarcadaRevision())
                .idempotenciaKey(aporte.getIdempotenciaKey())
                .build();
    }

    private static Aporte toDomain(AporteEntity entity) {
        return new Aporte(
                entity.getId(),
                entity.getAfiliadoId(),
                entity.getMonto(),
                entity.getFecha(),
                Canal.valueOf(entity.getCanal()),
                entity.getPeriodo(),
                entity.isMarcadaRevision(),
                entity.getIdempotenciaKey());
    }
}
