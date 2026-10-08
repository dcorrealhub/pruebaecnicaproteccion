package co.proteccion.cis.retob.infrastructure.persistence.adapter;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.infrastructure.persistence.entity.AporteEntity;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataAporteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador JPA para el puerto de salida {@link AporteRepositoryPort}.
 *
 * TODO (candidato): implementar los métodos mapeando entre
 * {@link co.proteccion.cis.retob.infrastructure.persistence.entity.AporteEntity}
 * y {@link Aporte}.
 */
@Repository
@RequiredArgsConstructor
public class JpaAporteRepositoryAdapter implements AporteRepositoryPort {

    private final SpringDataAporteRepository springDataRepo;
    @Override
    public Aporte guardar(Aporte aporte) {
        AporteEntity entity = toEntity(aporte);
        AporteEntity guardada = springDataRepo.save(entity);
        return toDomain(guardada);
    }

    @Override
    public Optional<Aporte> findByIdempotenciaKey(String idempotenciaKey) {
        return springDataRepo.findByIdempotenciaKey(idempotenciaKey)
                .map(this::toDomain);
    }
    @Override
    public List<Aporte> findByAfiliadoIdAndPeriodoBetween(String afiliadoId,
                                                           String periodoDesde,
                                                           String periodoHasta) {
        return springDataRepo.findByAfiliadoIdAndPeriodoBetween(afiliadoId, periodoDesde, periodoHasta)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private AporteEntity toEntity(Aporte aporte) {
        return AporteEntity.builder()
                .id(aporte.getId())
                .afiliadoId(aporte.getAfiliadoId())
                .monto(aporte.getMonto())
                .fecha(aporte.getFecha())
                .canal(aporte.getCanal())
                .periodo(aporte.getPeriodo())
                .marcadaRevision(aporte.isMarcadaRevision())
                .idempotenciaKey(aporte.getIdempotenciaKey())
                .build();
    }

    private Aporte toDomain(AporteEntity entity) {
        return new Aporte(
                entity.getId(),
                entity.getAfiliadoId(),
                entity.getMonto(),
                entity.getFecha(),
                entity.getCanal(),
                entity.getPeriodo(),
                entity.isMarcadaRevision(),
                entity.getIdempotenciaKey(),
                false // yaExistia: el adapter no decide esto, el caso de uso lo ajusta cuando corresponde
        );
    }
}
