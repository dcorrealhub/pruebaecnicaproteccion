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
 * Traduce entre la entidad de persistencia {@link AporteEntity} y el modelo de dominio {@link Aporte}.
 */
@Repository
@RequiredArgsConstructor
public class JpaAporteRepositoryAdapter implements AporteRepositoryPort {

    private final SpringDataAporteRepository springDataRepo;

    @Override
    public Aporte guardar(Aporte aporte) {
        AporteEntity guardada = springDataRepo.save(aEntidad(aporte));
        return aDominio(guardada);
    }

    @Override
    public Optional<Aporte> findById(Long id) {
        return springDataRepo.findById(id).map(this::aDominio);
    }

    @Override
    public Optional<Aporte> findByIdempotenciaKey(String idempotenciaKey) {
        return springDataRepo.findByIdempotenciaKey(idempotenciaKey).map(this::aDominio);
    }

    @Override
    public List<Aporte> findByAfiliadoIdAndPeriodoBetween(String afiliadoId,
                                                           String periodoDesde,
                                                           String periodoHasta) {
        return springDataRepo
                .findByAfiliadoIdAndPeriodoBetweenOrderByFechaAsc(afiliadoId, periodoDesde, periodoHasta)
                .stream()
                .map(this::aDominio)
                .toList();
    }

    private AporteEntity aEntidad(Aporte a) {
        return AporteEntity.builder()
                .id(a.getId())
                .afiliadoId(a.getAfiliadoId())
                .monto(a.getMonto())
                .fecha(a.getFecha())
                .canal(a.getCanal())
                .periodo(a.getPeriodo())
                .estado(a.getEstado())
                .marcadaRevision(a.isMarcadaRevision())
                .idempotenciaKey(a.getIdempotenciaKey())
                .build();
    }

    private Aporte aDominio(AporteEntity e) {
        return new Aporte(
                e.getId(),
                e.getAfiliadoId(),
                e.getMonto(),
                e.getFecha(),
                e.getCanal(),
                e.getPeriodo(),
                e.getEstado(),
                e.getIdempotenciaKey()
        );
    }
}
