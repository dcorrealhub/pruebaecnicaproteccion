package co.proteccion.cis.retob.domain.port.out;

import co.proteccion.cis.retob.domain.model.SaldoMensual;

import java.util.Optional;

/**
 * Puerto de salida: abstracción de persistencia para saldos mensuales.
 * La implementación debe garantizar control de concurrencia optimista.
 */
public interface SaldoRepositoryPort {

    Optional<SaldoMensual> findByAfiliadoIdAndMes(String afiliadoId, String mes);

    /**
     * Persiste el saldo. Si el {@code version} no coincide con el almacenado,
     * debe lanzar una excepción de conflicto de concurrencia.
     */
    SaldoMensual guardar(SaldoMensual saldo);

    SaldoMensual inicializar(String afiliadoId, String mes);
}
