package co.proteccion.cis.retob.infrastructure.persistence.repository;

import co.proteccion.cis.retob.infrastructure.persistence.entity.ParametroAporteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataParametroRepository extends JpaRepository<ParametroAporteEntity, Long> {

    Optional<ParametroAporteEntity> findByAfiliadoId(String afiliadoId);

    Optional<ParametroAporteEntity> findByAfiliadoIdIsNull();
}
