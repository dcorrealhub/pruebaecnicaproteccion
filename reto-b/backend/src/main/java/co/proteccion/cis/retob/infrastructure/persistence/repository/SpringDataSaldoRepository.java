package co.proteccion.cis.retob.infrastructure.persistence.repository;

import co.proteccion.cis.retob.infrastructure.persistence.entity.SaldoMensualEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataSaldoRepository extends JpaRepository<SaldoMensualEntity, Long> {

    Optional<SaldoMensualEntity> findByAfiliadoIdAndMes(String afiliadoId, String mes);
}
