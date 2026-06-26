package co.proteccion.cis.retob.infrastructure.persistence.adapter;

import co.proteccion.cis.retob.domain.model.HistorialEstadoAfiliado;
import co.proteccion.cis.retob.domain.port.out.HistorialEstadoAfiliadoRepository;
import co.proteccion.cis.retob.infrastructure.persistence.entity.HistorialEstadoAfiliadoEntity;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataHistorialEstadoAfiliadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class JpaHistorialEstadoAfiliadoRepositoryAdapter implements HistorialEstadoAfiliadoRepository {

    private final SpringDataHistorialEstadoAfiliadoRepository springDataRepo;

    @Override
    public List<HistorialEstadoAfiliado> findByAfiliadoId(String afiliadoId) {
        return springDataRepo.findByAfiliadoIdOrderByCambiadoEnDesc(afiliadoId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private HistorialEstadoAfiliado toDomain(HistorialEstadoAfiliadoEntity e) {
        return new HistorialEstadoAfiliado(
                e.getId().toString(),
                e.getAfiliadoId(),
                e.getEstadoAnterior(),
                e.getEstadoNuevo(),
                e.getCambiadoEn()
        );
    }
}
