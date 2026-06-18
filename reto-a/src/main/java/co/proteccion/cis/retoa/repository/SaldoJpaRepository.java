package co.proteccion.cis.retoa.repository;

import co.proteccion.cis.retoa.domain.Saldo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SaldoJpaRepository extends JpaRepository<Saldo, Long> {

    Optional<Saldo> findByAfiliadoId(String afiliadoId);
}
