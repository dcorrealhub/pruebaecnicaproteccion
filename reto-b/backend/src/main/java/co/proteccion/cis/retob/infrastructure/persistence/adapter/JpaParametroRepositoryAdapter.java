package co.proteccion.cis.retob.infrastructure.persistence.adapter;

import co.proteccion.cis.retob.domain.model.ParametrosFondo;
import co.proteccion.cis.retob.domain.port.out.ParametroRepository;
import co.proteccion.cis.retob.infrastructure.persistence.entity.ParametrosFondoEntity;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataParametrosRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JpaParametroRepositoryAdapter implements ParametroRepository {

    private final SpringDataParametrosRepository springDataRepo;

    @Override
    public ParametrosFondo guardarCambio(ParametrosFondo p) {
        return toDomain(springDataRepo.save(toEntity(p)));
    }

    @Override
    public Optional<ParametrosFondo> findLatest() {
        return springDataRepo.findTopByOrderByModificadoEnDesc().map(this::toDomain);
    }

    @Override
    public List<ParametrosFondo> findAll() {
        return springDataRepo.findAll().stream().map(this::toDomain).toList();
    }

    private ParametrosFondoEntity toEntity(ParametrosFondo p) {
        return ParametrosFondoEntity.builder()
                .id(p.getId() != null ? UUID.fromString(p.getId()) : null)
                .topeMensual(p.getTopeMensual())
                .umbralRevision(p.getUmbralRevision())
                .modificadoPor(p.getModificadoPor())
                .modificadoEn(p.getModificadoEn())
                .comentario(p.getComentario())
                .build();
    }

    private ParametrosFondo toDomain(ParametrosFondoEntity e) {
        return new ParametrosFondo(e.getId().toString(), e.getTopeMensual(), e.getUmbralRevision(),
                e.getModificadoPor(), e.getModificadoEn(), e.getComentario());
    }
}
