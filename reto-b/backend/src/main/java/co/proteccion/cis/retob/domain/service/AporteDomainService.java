package co.proteccion.cis.retob.domain.service;

import co.proteccion.cis.retob.domain.exception.CanalInvalidoException;
import co.proteccion.cis.retob.domain.exception.MontoInvalidoException;
import co.proteccion.cis.retob.domain.exception.PeriodoInvalidoException;
import co.proteccion.cis.retob.domain.exception.TopeMensualExcedidoException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

public final class AporteDomainService {

    private static final DateTimeFormatter PERIODO_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final Pattern PERIODO_PATTERN = Pattern.compile("\\d{4}-\\d{2}");

    public void validarMontoPositivo(BigDecimal monto) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new MontoInvalidoException(monto);
        }
    }

    public void validarTopeMensual(SaldoMensual saldo, BigDecimal monto, BigDecimal topeMensual) {
        BigDecimal totalProyectado = saldo.calcularNuevoTotal(monto);
        if (totalProyectado.compareTo(topeMensual) > 0) {
            throw new TopeMensualExcedidoException(
                    saldo.getAfiliadoId(), saldo.getMes(), totalProyectado, topeMensual);
        }
    }

    public EstadoAporte determinarEstado(BigDecimal monto, BigDecimal umbralRevision) {
        return monto.compareTo(umbralRevision) > 0
                ? EstadoAporte.REQUIERE_REVISION
                : EstadoAporte.REGISTRADO;
    }

    public String calcularPeriodo(LocalDate fecha) {
        return fecha.format(PERIODO_FORMAT);
    }

    public CanalAporte parseCanal(String canal) {
        if (canal == null || canal.isBlank()) {
            throw new CanalInvalidoException(canal);
        }
        try {
            return CanalAporte.valueOf(canal.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new CanalInvalidoException(canal);
        }
    }

    public void validarRangoPeriodos(String periodoDesde, String periodoHasta) {
        validarFormatoPeriodo(periodoDesde, "periodoDesde");
        validarFormatoPeriodo(periodoHasta, "periodoHasta");
        if (periodoDesde.compareTo(periodoHasta) > 0) {
            throw new PeriodoInvalidoException(
                    "periodoDesde (%s) no puede ser posterior a periodoHasta (%s)"
                            .formatted(periodoDesde, periodoHasta));
        }
    }

    private void validarFormatoPeriodo(String periodo, String campo) {
        if (periodo == null || periodo.isBlank()) {
            throw new PeriodoInvalidoException(campo + " es obligatorio");
        }
        String valor = periodo.trim();
        if (!PERIODO_PATTERN.matcher(valor).matches()) {
            throw new PeriodoInvalidoException(
                    campo + " debe tener formato YYYY-MM: " + periodo);
        }
        try {
            PERIODO_FORMAT.parse(valor);
        } catch (DateTimeParseException ex) {
            throw new PeriodoInvalidoException(
                    campo + " no es un periodo valido: " + periodo);
        }
    }
}
