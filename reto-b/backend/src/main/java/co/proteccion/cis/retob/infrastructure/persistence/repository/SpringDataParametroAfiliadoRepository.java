package co.proteccion.cis.retob.infrastructure.persistence.repository;

import co.proteccion.cis.retob.infrastructure.persistence.entity.ParametroAfiliadoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataParametroAfiliadoRepository extends JpaRepository<ParametroAfiliadoEntity, String> {
}
