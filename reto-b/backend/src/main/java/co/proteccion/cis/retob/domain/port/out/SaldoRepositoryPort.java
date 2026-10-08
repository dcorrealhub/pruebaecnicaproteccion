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
     * lanza {@link co.proteccion.cis.retob.domain.exception.ConcurrenciaConflictoException}.
     */
    SaldoMensual guardar(SaldoMensual saldo);

    /**
     * Crea el saldo del mes con total cero. Si otra operación concurrente ya lo creó,
     * lanza {@link co.proteccion.cis.retob.domain.exception.ConcurrenciaConflictoException}.
     */
    SaldoMensual inicializar(String afiliadoId, String mes);
}
