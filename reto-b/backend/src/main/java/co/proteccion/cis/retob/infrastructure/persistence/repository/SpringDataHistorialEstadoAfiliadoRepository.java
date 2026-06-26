package co.proteccion.cis.retob.infrastructure.persistence.repository;

import co.proteccion.cis.retob.infrastructure.persistence.entity.HistorialEstadoAfiliadoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataHistorialEstadoAfiliadoRepository
        extends JpaRepository<HistorialEstadoAfiliadoEntity, UUID> {

    List<HistorialEstadoAfiliadoEntity> findByAfiliadoIdOrderByCambiadoEnDesc(String afiliadoId);
}
