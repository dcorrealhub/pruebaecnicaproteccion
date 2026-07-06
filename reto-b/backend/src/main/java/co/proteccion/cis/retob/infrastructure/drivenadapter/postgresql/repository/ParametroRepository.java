package co.proteccion.cis.retob.infrastructure.drivenadapter.postgresql.repository;

import co.proteccion.cis.retob.infrastructure.drivenadapter.postgresql.entity.ParametroAporteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParametroRepository extends JpaRepository<ParametroAporteEntity, Long> {

    Optional<ParametroAporteEntity> findByAfiliadoId(String afiliadoId);

    Optional<ParametroAporteEntity> findByAfiliadoIdIsNull();
}
