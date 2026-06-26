package co.proteccion.cis.retob.infrastructure.persistence.repository;

import co.proteccion.cis.retob.infrastructure.persistence.entity.AporteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataAporteRepository extends JpaRepository<AporteEntity, UUID> {

    Optional<AporteEntity> findByIdempotenciaKey(String idempotenciaKey);

    List<AporteEntity> findByAfiliadoIdAndPeriodoBetween(String afiliadoId,
                                                          String periodoDesde,
                                                          String periodoHasta);
}
