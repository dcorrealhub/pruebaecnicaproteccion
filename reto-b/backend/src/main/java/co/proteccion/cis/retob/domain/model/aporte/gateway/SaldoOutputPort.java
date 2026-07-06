package co.proteccion.cis.retob.domain.model.aporte.gateway;

import co.proteccion.cis.retob.domain.model.aporte.SaldoMensual;

import java.util.Optional;

public interface SaldoOutputPort {

    Optional<SaldoMensual> findByAfiliadoIdAndMes(String afiliadoId, String mes);

    SaldoMensual save(SaldoMensual saldo);

    SaldoMensual inicializar(String afiliadoId, String mes);
}
