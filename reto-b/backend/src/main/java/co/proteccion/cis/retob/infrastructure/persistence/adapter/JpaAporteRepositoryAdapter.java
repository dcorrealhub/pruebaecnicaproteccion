package co.proteccion.cis.retob.infrastructure.persistence.adapter;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.port.out.AporteRepositoryPort;
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
        // TODO: mapear Aporte → AporteEntity, guardar, mapear AporteEntity → Aporte
        throw new UnsupportedOperationException("Pendiente de implementación");
    }

    @Override
    public Optional<Aporte> findByIdempotenciaKey(String idempotenciaKey) {
        // TODO: buscar por idempotenciaKey y mapear resultado
        throw new UnsupportedOperationException("Pendiente de implementación");
    }

    @Override
    public List<Aporte> findByAfiliadoIdAndPeriodoBetween(String afiliadoId,
                                                           String periodoDesde,
                                                           String periodoHasta) {
        // TODO: delegar en springDataRepo y mapear la lista
        throw new UnsupportedOperationException("Pendiente de implementación");
    }
}
