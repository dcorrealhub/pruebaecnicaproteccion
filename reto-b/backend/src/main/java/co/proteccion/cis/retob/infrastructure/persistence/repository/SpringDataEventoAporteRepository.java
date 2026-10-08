package co.proteccion.cis.retob.infrastructure.persistence.repository;

import co.proteccion.cis.retob.infrastructure.persistence.entity.EventoAporteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataEventoAporteRepository extends JpaRepository<EventoAporteEntity, Long> {

    List<EventoAporteEntity> findByAporteId(Long aporteId);
}
