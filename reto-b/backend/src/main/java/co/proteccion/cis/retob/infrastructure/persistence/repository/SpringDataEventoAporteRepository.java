package co.proteccion.cis.retob.infrastructure.persistence.repository;

import co.proteccion.cis.retob.infrastructure.persistence.entity.EventoAporteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataEventoAporteRepository extends JpaRepository<EventoAporteEntity, Long> {
}
