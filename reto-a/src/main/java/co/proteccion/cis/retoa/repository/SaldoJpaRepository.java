package co.proteccion.cis.retoa.repository;

import co.proteccion.cis.retoa.domain.Saldo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SaldoJpaRepository extends JpaRepository<Saldo, Long> {

    Optional<Saldo> findByAfiliadoIdAndMes(String afiliadoId, String mes);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Saldo s WHERE s.afiliadoId = :afiliadoId")
    Optional<Saldo> findByAfiliadoIdWithLock(@Param("afiliadoId") String afiliadoId);
}
