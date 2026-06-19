package co.proteccion.cis.retob.domain.port.out;

import co.proteccion.cis.retob.domain.model.SaldoMensual;

import java.util.Optional;

/**
 * Puerto de salida: persistencia del saldo mensual acumulado por afiliado.
 */
public interface SaldoRepositoryPort {

    Optional<SaldoMensual> findByAfiliadoIdAndMes(String afiliadoId, String mes);

    SaldoMensual guardar(SaldoMensual saldo);

    /**
     * Crea un saldo mensual inicial en cero para el afiliado y mes indicados.
     * Solo debe llamarse cuando no existe saldo previo para ese periodo.
     */
    SaldoMensual inicializar(String afiliadoId, String mes);
}
