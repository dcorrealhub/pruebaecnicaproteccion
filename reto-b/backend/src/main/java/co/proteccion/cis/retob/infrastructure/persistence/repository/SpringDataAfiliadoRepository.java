package co.proteccion.cis.retob.infrastructure.persistence.repository;

import co.proteccion.cis.retob.infrastructure.persistence.entity.AfiliadoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataAfiliadoRepository extends JpaRepository<AfiliadoEntity, Long> {

    Optional<AfiliadoEntity> findByAfiliadoId(String afiliadoId);
}
