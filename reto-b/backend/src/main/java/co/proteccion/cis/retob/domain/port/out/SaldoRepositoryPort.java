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
     * lanza {@link co.proteccion.cis.retob.domain.exception.ConflictoConcurrenciaException}.
     */
    SaldoMensual guardar(SaldoMensual saldo);

    /**
     * Crea el saldo del mes en cero. Si otra transacción lo creó en paralelo
     * (restricción única afiliado+mes), lanza
     * {@link co.proteccion.cis.retob.domain.exception.ConflictoConcurrenciaException}.
     */
    SaldoMensual inicializar(String afiliadoId, String mes);
}
