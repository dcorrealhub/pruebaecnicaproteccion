package co.proteccion.cis.retob.infrastructure.persistence.repository;

import co.proteccion.cis.retob.infrastructure.persistence.entity.RevisionAporteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataRevisionRepository extends JpaRepository<RevisionAporteEntity, Long> {

    List<RevisionAporteEntity> findByAporteId(Long aporteId);
}
