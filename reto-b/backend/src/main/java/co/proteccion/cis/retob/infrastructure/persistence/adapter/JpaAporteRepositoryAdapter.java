package co.proteccion.cis.retob.infrastructure.persistence.adapter;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
import co.proteccion.cis.retob.infrastructure.persistence.mapper.AportePersistenceMapper;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataAporteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaAporteRepositoryAdapter implements AporteRepositoryPort {

    private final SpringDataAporteRepository springDataRepo;
    private final AportePersistenceMapper mapper;

    @Override
    public Aporte guardar(Aporte aporte) {
        try {
            var entity = mapper.toEntity(aporte);
            var saved = springDataRepo.save(entity);
            return mapper.toDomain(saved);
        } catch (DataIntegrityViolationException ex) {
            return springDataRepo.findByIdempotenciaKey(aporte.getIdempotenciaKey())
                    .map(mapper::toDomain)
                    .orElseThrow(() -> ex);
        }
    }

    @Override
    public Optional<Aporte> findById(Long id) {
        return springDataRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Aporte> findByIdempotenciaKey(String idempotenciaKey) {
        return springDataRepo.findByIdempotenciaKey(idempotenciaKey).map(mapper::toDomain);
    }

    @Override
    public List<Aporte> findByAfiliadoIdAndPeriodoBetween(String afiliadoId,
                                                          String periodoDesde,
                                                          String periodoHasta) {
        return springDataRepo.findByAfiliadoIdAndPeriodoBetweenOrderByFechaAsc(
                        afiliadoId, periodoDesde, periodoHasta)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public ConsolidadoConsulta buscarConsolidado(String afiliadoId,
                                                 String periodoDesde,
                                                 String periodoHasta) {
        BigDecimal total = springDataRepo.sumMontoByAfiliadoIdAndPeriodoBetween(
                afiliadoId, periodoDesde, periodoHasta);
        List<Aporte> aportes = springDataRepo.findByAfiliadoIdAndPeriodoBetweenOrderByFechaAsc(
                        afiliadoId, periodoDesde, periodoHasta)
                .stream()
                .map(mapper::toDomain)
                .toList();
        return new ConsolidadoConsulta(total, aportes);
    }
}
