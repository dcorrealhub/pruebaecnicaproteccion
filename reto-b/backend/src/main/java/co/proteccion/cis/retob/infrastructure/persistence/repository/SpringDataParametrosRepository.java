package co.proteccion.cis.retob.infrastructure.persistence.repository;

import co.proteccion.cis.retob.infrastructure.persistence.entity.ParametrosFondoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataParametrosRepository extends JpaRepository<ParametrosFondoEntity, UUID> {

    Optional<ParametrosFondoEntity> findTopByOrderByModificadoEnDesc();
}
