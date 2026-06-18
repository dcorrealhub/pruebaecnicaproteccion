package co.proteccion.cis.retoa.repository;

import co.proteccion.cis.retoa.domain.Aporte;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AporteJpaRepository extends JpaRepository<Aporte, Long> {

    List<Aporte> findByAfiliadoIdAndPeriodo(String afiliadoId, String periodo);
}
