package co.proteccion.cis.retob.infrastructure.persistence.adapter;

import co.proteccion.cis.retob.domain.model.RevisionAporte;
import co.proteccion.cis.retob.domain.port.out.RevisionRepository;
import co.proteccion.cis.retob.infrastructure.persistence.entity.AporteEntity;
import co.proteccion.cis.retob.infrastructure.persistence.entity.RevisionAporteEntity;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataRevisionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JpaRevisionRepositoryAdapter implements RevisionRepository {

    private final SpringDataRevisionRepository springDataRepo;

    @Override
    public RevisionAporte guardar(RevisionAporte revision) {
        AporteEntity aporteRef = new AporteEntity();
        aporteRef.setId(UUID.fromString(revision.getAporteId()));

        RevisionAporteEntity entity = RevisionAporteEntity.builder()
                .id(revision.getId() != null ? UUID.fromString(revision.getId()) : null)
                .aporte(aporteRef)
                .revisor(revision.getRevisor())
                .decision(revision.getDecision())
                .comentario(revision.getComentario())
                .ocurridoEn(revision.getOcurridoEn())
                .build();
        return toDomain(springDataRepo.save(entity));
    }

    @Override
    public List<RevisionAporte> findByAporteId(String aporteId) {
        return springDataRepo.findByAporte_Id(UUID.fromString(aporteId))
                .stream().map(this::toDomain).toList();
    }

    private RevisionAporte toDomain(RevisionAporteEntity e) {
        return new RevisionAporte(e.getId().toString(), e.getAporte().getId().toString(),
                e.getRevisor(), e.getDecision(), e.getComentario(), e.getOcurridoEn());
    }
}
