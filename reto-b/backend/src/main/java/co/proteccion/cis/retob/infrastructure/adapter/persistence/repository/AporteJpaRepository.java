package co.proteccion.cis.retob.infrastructure.adapter.persistence.repository;

import co.proteccion.cis.retob.infrastructure.adapter.persistence.entity.AporteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AporteJpaRepository extends JpaRepository<AporteEntity, Long> {

    Optional<AporteEntity> findByIdempotenciaKey(String idempotenciaKey);

    java.util.List<AporteEntity> findByAfiliadoIdAndPeriodoBetween(
            String afiliadoId, String desde, String hasta);
}