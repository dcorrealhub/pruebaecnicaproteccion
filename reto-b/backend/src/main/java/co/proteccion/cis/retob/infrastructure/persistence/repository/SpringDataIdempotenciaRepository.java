package co.proteccion.cis.retob.infrastructure.persistence.repository;

import co.proteccion.cis.retob.infrastructure.persistence.entity.IdempotenciaAporteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataIdempotenciaRepository extends JpaRepository<IdempotenciaAporteEntity, Long> {

    Optional<IdempotenciaAporteEntity> findByIdempotenciaKey(String idempotenciaKey);

    void deleteByIdempotenciaKey(String idempotenciaKey);
}
