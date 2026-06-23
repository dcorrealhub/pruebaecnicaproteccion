package co.proteccion.cis.retob.infrastructure.persistence.repository;

import co.proteccion.cis.retob.infrastructure.persistence.entity.AporteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface SpringDataAporteRepository extends JpaRepository<AporteEntity, Long> {

    Optional<AporteEntity> findByIdempotenciaKey(String idempotenciaKey);

    List<AporteEntity> findByAfiliadoIdAndPeriodoBetweenOrderByFechaAsc(String afiliadoId,
                                                                          String periodoDesde,
                                                                          String periodoHasta);

    @Query("""
            SELECT COALESCE(SUM(a.monto), 0)
            FROM AporteEntity a
            WHERE a.afiliadoId = :afiliadoId
              AND a.periodo BETWEEN :periodoDesde AND :periodoHasta
            """)
    BigDecimal sumMontoByAfiliadoIdAndPeriodoBetween(@Param("afiliadoId") String afiliadoId,
                                                     @Param("periodoDesde") String periodoDesde,
                                                     @Param("periodoHasta") String periodoHasta);
}
