package co.proteccion.cis.retob.infrastructure.persistence.adapter;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.infrastructure.persistence.entity.AporteEntity;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataAporteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaAporteRepositoryAdapter implements AporteRepositoryPort {

    private final SpringDataAporteRepository springDataRepo;

    @Override
    public Aporte guardar(Aporte aporte) {
        AporteEntity entity = toEntity(aporte);
        AporteEntity saved = springDataRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Aporte> findByIdempotenciaKey(String idempotenciaKey) {
        return springDataRepo.findByIdempotenciaKey(idempotenciaKey)
                .map(JpaAporteRepositoryAdapter::toDomain);
    }

    @Override
    public List<Aporte> findByAfiliadoIdAndPeriodoBetween(String afiliadoId,
                                                           String periodoDesde,
                                                           String periodoHasta) {
        return springDataRepo.findByAfiliadoIdAndPeriodoBetween(afiliadoId, periodoDesde, periodoHasta)
                .stream()
                .map(JpaAporteRepositoryAdapter::toDomain)
                .toList();
    }

    private static AporteEntity toEntity(Aporte domain) {
        return AporteEntity.builder()
                .id(domain.getId())
                .afiliadoId(domain.getAfiliadoId())
                .monto(domain.getMonto())
                .fecha(domain.getFecha())
                .canal(domain.getCanal())
                .periodo(domain.getPeriodo())
                .marcadaRevision(domain.isMarcadaRevision())
                .idempotenciaKey(domain.getIdempotenciaKey())
                .build();
    }

    private static Aporte toDomain(AporteEntity entity) {
        return new Aporte(
                entity.getId(),
                entity.getAfiliadoId(),
                entity.getMonto(),
                entity.getFecha(),
                entity.getCanal(),
                entity.getPeriodo(),
                entity.isMarcadaRevision(),
                entity.getIdempotenciaKey()
        );
    }
}
