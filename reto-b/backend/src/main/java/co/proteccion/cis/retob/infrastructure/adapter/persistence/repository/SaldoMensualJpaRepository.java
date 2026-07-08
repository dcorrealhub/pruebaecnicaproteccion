package co.proteccion.cis.retob.infrastructure.adapter.persistence.repository;

import co.proteccion.cis.retob.infrastructure.adapter.persistence.entity.SaldoMensualEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SaldoMensualJpaRepository extends JpaRepository<SaldoMensualEntity, Long> {

    Optional<SaldoMensualEntity> findByAfiliadoIdAndMes(String afiliadoId, String mes);

    @Modifying
    @Query("INSERT INTO SaldoMensualEntity (afiliadoId, mes, total, version) VALUES (:afiliadoId, :mes, 0, 0)")
    int inicializarSiNoExiste(@Param("afiliadoId") String afiliadoId, @Param("mes") String mes);
}