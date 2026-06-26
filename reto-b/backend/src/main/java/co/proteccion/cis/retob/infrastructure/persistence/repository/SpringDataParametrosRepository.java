package co.proteccion.cis.retob.infrastructure.persistence.repository;

import co.proteccion.cis.retob.infrastructure.persistence.entity.ParametrosFondoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataParametrosRepository extends JpaRepository<ParametrosFondoEntity, Long> {

    Optional<ParametrosFondoEntity> findTopByOrderByModificadoEnDesc();
}
