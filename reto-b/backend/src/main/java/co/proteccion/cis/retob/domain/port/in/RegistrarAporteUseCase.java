package co.proteccion.cis.retob.domain.port.in;

import co.proteccion.cis.retob.domain.model.Aporte;

import java.math.BigDecimal;

public interface RegistrarAporteUseCase {

    Aporte registrar(String afiliadoId, BigDecimal monto, String canal, String idempotenciaKey);
}
