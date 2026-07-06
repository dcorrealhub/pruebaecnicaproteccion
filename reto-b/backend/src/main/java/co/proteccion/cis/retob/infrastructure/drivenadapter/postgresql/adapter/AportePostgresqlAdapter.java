package co.proteccion.cis.retob.infrastructure.drivenadapter.postgresql.adapter;

import co.proteccion.cis.retob.domain.model.aporte.Aporte;
import co.proteccion.cis.retob.domain.model.aporte.gateway.AporteOutputPort;
import co.proteccion.cis.retob.infrastructure.drivenadapter.postgresql.mapper.AporteEntityMapper;
import co.proteccion.cis.retob.infrastructure.drivenadapter.postgresql.repository.AporteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AportePostgresqlAdapter implements AporteOutputPort {

    private final AporteRepository repository;
    private final AporteEntityMapper mapper;

    @Override
    public Aporte save(Aporte aporte) {
        return mapper.toDomain(repository.save(mapper.toEntity(aporte)));
    }

    @Override
    public Optional<Aporte> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Aporte> findByIdempotenciaKey(String idempotenciaKey) {
        return repository.findByIdempotenciaKey(idempotenciaKey).map(mapper::toDomain);
    }

    @Override
    public List<Aporte> findByAfiliadoIdAndPeriodoBetween(String afiliadoId,
                                                          String periodoDesde,
                                                          String periodoHasta) {
        return mapper.toDomainList(
                repository.findByAfiliadoIdAndPeriodoBetweenOrderByFechaAsc(afiliadoId, periodoDesde, periodoHasta));
    }
}
