package co.proteccion.cis.retob.infrastructure.adapter.persistence;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.infrastructure.adapter.persistence.entity.AporteEntity;
import co.proteccion.cis.retob.infrastructure.adapter.persistence.mapper.AporteMapper;
import co.proteccion.cis.retob.infrastructure.adapter.persistence.repository.AporteJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class AporteRepositoryAdapter implements AporteRepositoryPort {

    private final AporteJpaRepository jpaRepository;
    private final AporteMapper mapper;

    public AporteRepositoryAdapter(AporteJpaRepository jpaRepository, AporteMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Aporte guardar(Aporte aporte) {
        AporteEntity entity = mapper.toEntity(aporte);
        AporteEntity guardado = jpaRepository.save(entity);
        return mapper.toDomain(guardado);
    }

    @Override
    public Optional<Aporte> findByIdempotenciaKey(String idempotenciaKey) {
        return jpaRepository.findByIdempotenciaKey(idempotenciaKey).map(mapper::toDomain);
    }

    @Override
    public List<Aporte> findByAfiliadoIdAndPeriodoBetween(String afiliadoId, String desde, String hasta) {
        return jpaRepository.findByAfiliadoIdAndPeriodoBetween(afiliadoId, desde, hasta)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}