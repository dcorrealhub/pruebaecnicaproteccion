package co.proteccion.cis.retob.infrastructure.drivenadapter.postgresql.repository;

import co.proteccion.cis.retob.infrastructure.drivenadapter.postgresql.entity.EventoAporteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoRepository extends JpaRepository<EventoAporteEntity, Long> {
}
