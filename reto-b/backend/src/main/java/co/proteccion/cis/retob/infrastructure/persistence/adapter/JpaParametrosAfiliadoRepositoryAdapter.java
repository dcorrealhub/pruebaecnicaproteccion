package co.proteccion.cis.retob.infrastructure.persistence.adapter;

import co.proteccion.cis.retob.domain.model.ParametrosAfiliado;
import co.proteccion.cis.retob.domain.port.out.ParametrosAfiliadoRepositoryPort;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataParametroAfiliadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Adaptador JPA para el puerto de salida {@link ParametrosAfiliadoRepositoryPort}.
 */
@Repository
@RequiredArgsConstructor
public class JpaParametrosAfiliadoRepositoryAdapter implements ParametrosAfiliadoRepositoryPort {

    private final SpringDataParametroAfiliadoRepository springDataRepo;

    @Override
    public Optional<ParametrosAfiliado> findByAfiliadoId(String afiliadoId) {
        return springDataRepo.findById(afiliadoId)
                .map(e -> new ParametrosAfiliado(e.getTopeMensual(), e.getUmbralRevision()));
    }
}
