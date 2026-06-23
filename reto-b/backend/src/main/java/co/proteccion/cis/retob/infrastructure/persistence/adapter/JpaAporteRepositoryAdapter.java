package co.proteccion.cis.retob.infrastructure.persistence.adapter;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.infrastructure.persistence.entity.AporteEntity;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataAporteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    // TODO: mapear Aporte → AporteEntity, guardar, mapear AporteEntity → Aporte
    @Override
    public Aporte guardar(Aporte aporte) {
        AporteEntity entity = toEntity(aporte);
        AporteEntity guardado = springDataRepo.save(entity);
        return toDomain(guardado);
    }

    // TODO: buscar por idempotenciaKey y mapear resultado
    @Override
    public Optional<Aporte> findByIdempotenciaKey(String idempotenciaKey) {
        return springDataRepo.findByIdempotenciaKey(idempotenciaKey)
                .map(this::toDomain);
    }

    // TODO: delegar en springDataRepo y mapear la lista
    @Override
    public List<Aporte> findByAfiliadoIdAndPeriodoBetween(String afiliadoId,
                                                          String periodoDesde,
                                                          String periodoHasta) {
        return springDataRepo
                .findByAfiliadoIdAndPeriodoBetween(afiliadoId, periodoDesde, periodoHasta)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }


    private AporteEntity toEntity(Aporte a) {
        return AporteEntity.builder()
                .id(a.getId())
                .afiliadoId(a.getAfiliadoId())
                .monto(a.getMonto())
                .fecha(a.getFecha())
                .canal(a.getCanal())
                .periodo(a.getPeriodo())
                .marcadaRevision(a.isMarcadaRevision())
                .idempotenciaKey(a.getIdempotenciaKey())
                .build();
    }

    private Aporte toDomain(AporteEntity e) {
        return new Aporte(
                e.getId(),
                e.getAfiliadoId(),
                e.getMonto(),
                e.getFecha(),
                e.getCanal(),
                e.getPeriodo(),
                e.isMarcadaRevision(),
                e.getIdempotenciaKey()
        );
    }
}