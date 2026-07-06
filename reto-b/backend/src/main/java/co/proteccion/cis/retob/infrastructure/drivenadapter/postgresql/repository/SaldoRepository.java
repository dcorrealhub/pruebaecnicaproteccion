package co.proteccion.cis.retob.infrastructure.drivenadapter.postgresql.repository;

import co.proteccion.cis.retob.infrastructure.drivenadapter.postgresql.entity.SaldoMensualEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SaldoRepository extends JpaRepository<SaldoMensualEntity, Long> {

    Optional<SaldoMensualEntity> findByAfiliadoIdAndMes(String afiliadoId, String mes);
}
