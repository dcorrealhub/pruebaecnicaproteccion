package co.proteccion.cis.retob.support;

import co.proteccion.cis.retob.domain.model.Aporte;
import co.proteccion.cis.retob.domain.model.SaldoMensual;
import co.proteccion.cis.retob.domain.port.in.RegistrarAporteUseCase.RegistrarAporteCommand;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class AporteMother {

    private AporteMother() {}

    public static Aporte normal() {
        return new Aporte(1L, "AF-001", new BigDecimal("100000.00"),
                LocalDate.of(2026, 6, 1), "WEB", "2026-06", false, "idem-normal-001");
    }

    public static Aporte granMonto() {
        return new Aporte(2L, "AF-002", new BigDecimal("6000000.00"),
                LocalDate.of(2026, 6, 1), "APP_MOVIL", "2026-06", true, "idem-gran-001");
    }

    public static RegistrarAporteCommand command(String afiliadoId, String monto, String canal, String key) {
        return new RegistrarAporteCommand(afiliadoId, new BigDecimal(monto), canal, key);
    }

    public static SaldoMensual saldoEnCero(String afiliadoId, String mes) {
        return new SaldoMensual(1L, afiliadoId, mes, BigDecimal.ZERO, 0);
    }

    public static SaldoMensual saldoCon(String afiliadoId, String mes, String total) {
        return new SaldoMensual(1L, afiliadoId, mes, new BigDecimal(total), 0);
    }
}
