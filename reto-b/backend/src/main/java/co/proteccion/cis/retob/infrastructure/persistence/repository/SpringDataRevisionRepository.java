package co.proteccion.cis.retob.infrastructure.persistence.repository;

import co.proteccion.cis.retob.infrastructure.persistence.entity.RevisionAporteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataRevisionRepository extends JpaRepository<RevisionAporteEntity, UUID> {

    // Navega RevisionAporteEntity.aporte.id
    List<RevisionAporteEntity> findByAporte_Id(UUID aporteId);
}
