package co.proteccion.cis.retob.infrastructure.persistence.adapter;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.infrastructure.persistence.entity.AporteEntity;
import co.proteccion.cis.retob.infrastructure.persistence.mapper.AporteEntityMapper;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataAporteRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador JPA para el puerto de salida {@link AporteRepositoryPort}.
 * Mapea entre {@link AporteEntity} y el modelo de dominio {@link Aporte}.
 */
@Repository
public class JpaAporteRepositoryAdapter implements AporteRepositoryPort {

    private final SpringDataAporteRepository springDataRepo;

    public JpaAporteRepositoryAdapter(SpringDataAporteRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    @Override
    public Aporte guardar(Aporte aporte) {
        AporteEntity guardada = springDataRepo.save(AporteEntityMapper.aEntidad(aporte));
        return AporteEntityMapper.aDominio(guardada);
    }

    @Override
    public Optional<Aporte> findById(Long id) {
        return springDataRepo.findById(id).map(AporteEntityMapper::aDominio);
    }

    @Override
    public Optional<Aporte> findByIdempotenciaKey(String idempotenciaKey) {
        return springDataRepo.findByIdempotenciaKey(idempotenciaKey)
                .map(AporteEntityMapper::aDominio);
    }

    @Override
    public List<Aporte> findByAfiliadoIdAndPeriodoBetween(String afiliadoId,
                                                           String periodoDesde,
                                                           String periodoHasta) {
        return springDataRepo
                .findByAfiliadoIdAndPeriodoBetween(afiliadoId, periodoDesde, periodoHasta)
                .stream()
                .map(AporteEntityMapper::aDominio)
                .toList();
    }
}
