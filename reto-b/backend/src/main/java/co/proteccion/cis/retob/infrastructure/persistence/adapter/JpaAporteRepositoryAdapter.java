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

    private AporteEntity toEntity(Aporte a) {
        AporteEntity entity = new AporteEntity();
        entity.setId(a.getId());
        entity.setAfiliadoId(a.getAfiliadoId());
        entity.setMonto(a.getMonto());
        entity.setFecha(a.getFecha());
        entity.setCanal(a.getCanal());
        entity.setPeriodo(a.getPeriodo());
        entity.setMarcadaRevision(a.isMarcadaRevision());
        entity.setIdempotenciaKey(a.getIdempotenciaKey());
        return entity;
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
