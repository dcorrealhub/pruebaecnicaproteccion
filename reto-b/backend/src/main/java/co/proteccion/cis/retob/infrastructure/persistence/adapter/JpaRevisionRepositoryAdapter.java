package co.proteccion.cis.retob.infrastructure.persistence.adapter;

import co.proteccion.cis.retob.domain.model.RevisionAporte;
import co.proteccion.cis.retob.domain.port.out.RevisionRepository;
import co.proteccion.cis.retob.infrastructure.persistence.entity.AporteEntity;
import co.proteccion.cis.retob.infrastructure.persistence.entity.RevisionAporteEntity;
import co.proteccion.cis.retob.infrastructure.persistence.repository.SpringDataRevisionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class JpaRevisionRepositoryAdapter implements RevisionRepository {

    private final SpringDataRevisionRepository springDataRepo;

    @Override
    public RevisionAporte guardar(RevisionAporte revision) {
        AporteEntity aporteRef = new AporteEntity();
        aporteRef.setId(revision.getAporteId());

        RevisionAporteEntity entity = RevisionAporteEntity.builder()
                .id(revision.getId())
                .aporte(aporteRef)
                .revisor(revision.getRevisor())
                .decision(revision.getDecision())
                .comentario(revision.getComentario())
                .ocurridoEn(revision.getOcurridoEn())
                .build();
        return toDomain(springDataRepo.save(entity));
    }

    @Override
    public List<RevisionAporte> findByAporteId(Long aporteId) {
        return springDataRepo.findByAporteId(aporteId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private RevisionAporte toDomain(RevisionAporteEntity e) {
        return new RevisionAporte(
                e.getId(),
                e.getAporte().getId(),
                e.getRevisor(),
                e.getDecision(),
                e.getComentario(),
                e.getOcurridoEn()
        );
    }
}
