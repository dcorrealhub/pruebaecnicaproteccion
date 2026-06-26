package co.proteccion.cis.retob.infrastructure.persistence.adapter;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.infrastructure.persistence.entity.AporteEntity;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataAporteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JpaAporteRepositoryAdapter implements AporteRepositoryPort {

    private final SpringDataAporteRepository springDataRepo;

    @Override
    public Aporte guardar(Aporte aporte) {
        return toDomain(springDataRepo.save(toEntity(aporte)));
    }

    @Override
    public Optional<Aporte> findById(String id) {
        return springDataRepo.findById(UUID.fromString(id)).map(this::toDomain);
    }

    @Override
    public Optional<Aporte> findByIdempotenciaKey(String idempotenciaKey) {
        return springDataRepo.findByIdempotenciaKey(idempotenciaKey).map(this::toDomain);
    }

    @Override
    public List<Aporte> findByAfiliadoIdAndPeriodoBetween(String afiliadoId, String periodoDesde, String periodoHasta) {
        return springDataRepo.findByAfiliadoIdAndPeriodoBetween(afiliadoId, periodoDesde, periodoHasta)
                .stream().map(this::toDomain).toList();
    }

    private AporteEntity toEntity(Aporte a) {
        return AporteEntity.builder()
                .id(a.getId() != null ? UUID.fromString(a.getId()) : null)
                .afiliadoId(a.getAfiliadoId())
                .monto(a.getMonto())
                .fecha(a.getFecha())
                .canal(a.getCanal())
                .periodo(a.getPeriodo())
                .estado(a.getEstado())
                .idempotenciaKey(a.getIdempotenciaKey())
                .build();
    }

    private Aporte toDomain(AporteEntity e) {
        return new Aporte(e.getId().toString(), e.getAfiliadoId(), e.getMonto(), e.getFecha(),
                e.getCanal(), e.getPeriodo(), e.getEstado(), e.getIdempotenciaKey());
    }
}
