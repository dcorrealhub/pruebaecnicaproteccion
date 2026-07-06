package co.proteccion.cis.retob.infrastructure.drivenadapter.postgresql.repository;

import co.proteccion.cis.retob.infrastructure.drivenadapter.postgresql.entity.AporteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AporteRepository extends JpaRepository<AporteEntity, Long> {

    Optional<AporteEntity> findByIdempotenciaKey(String idempotenciaKey);

    List<AporteEntity> findByAfiliadoIdAndPeriodoBetweenOrderByFechaAsc(String afiliadoId,
                                                                        String periodoDesde,
                                                                        String periodoHasta);
}
