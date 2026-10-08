package co.proteccion.cis.retob.domain.service;

import co.proteccion.cis.retob.domain.exception.ReglaNegocioException;
import co.proteccion.cis.retob.domain.model.Canal;
import co.proteccion.cis.retob.domain.model.ParametrosAfiliado;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Reglas de negocio de un aporte voluntario. Java puro: los valores por defecto llegan
 * por constructor (configurados en infraestructura), lo que permite probarla sin Spring.
 *
 * <ul>
 *   <li>El monto debe ser positivo y tener a lo sumo 2 decimales.</li>
 *   <li>Cada afiliado puede tener su propio tope mensual; si no tiene, aplica el tope por defecto.
 *       El acumulado del mes más el nuevo monto no puede superarlo (llegar exacto al tope es válido).</li>
 *   <li>Un monto estrictamente mayor al umbral de revisión queda marcado para revisión. El umbral se
 *       resuelve en este orden: umbral del canal (canales de mayor riesgo, p. ej. SUCURSAL) → umbral
 *       propio del afiliado → umbral por defecto. El umbral del canal prevalece sobre el del afiliado.</li>
 * </ul>
 */
public class PoliticaAportes {

    private static final int ESCALA_MAXIMA = 2;

    private final BigDecimal topeMensualPorDefecto;
    private final BigDecimal umbralRevisionPorDefecto;
    private final Map<Canal, BigDecimal> umbralRevisionPorCanal;

    public PoliticaAportes(BigDecimal topeMensualPorDefecto,
                           BigDecimal umbralRevisionPorDefecto,
                           Map<Canal, BigDecimal> umbralRevisionPorCanal) {
        this.topeMensualPorDefecto = Objects.requireNonNull(topeMensualPorDefecto, "topeMensualPorDefecto");
        this.umbralRevisionPorDefecto = Objects.requireNonNull(umbralRevisionPorDefecto, "umbralRevisionPorDefecto");
        this.umbralRevisionPorCanal = umbralRevisionPorCanal.isEmpty()
                ? Map.of()
                : new EnumMap<>(umbralRevisionPorCanal);
        if (topeMensualPorDefecto.signum() <= 0 || umbralRevisionPorDefecto.signum() <= 0
                || this.umbralRevisionPorCanal.values().stream().anyMatch(u -> u == null || u.signum() <= 0)) {
            throw new IllegalArgumentException("El tope mensual y los umbrales de revisión deben ser positivos");
        }
    }

    public void validarMonto(BigDecimal monto) {
        if (monto == null || monto.signum() <= 0) {
            throw new ReglaNegocioException("MONTO_INVALIDO", "El monto debe ser mayor a cero");
        }
        if (monto.stripTrailingZeros().scale() > ESCALA_MAXIMA) {
            throw new ReglaNegocioException("MONTO_INVALIDO", "El monto admite máximo 2 decimales");
        }
    }

    /** Tope del afiliado: el suyo propio o, si no tiene, el tope por defecto. */
    public BigDecimal topeAplicable(ParametrosAfiliado parametros) {
        return parametros.topeMensual() != null ? parametros.topeMensual() : topeMensualPorDefecto;
    }

    /**
     * Umbral de revisión para un aporte: el del canal si el canal tiene uno definido;
     * si no, el propio del afiliado; si no, el umbral por defecto.
     */
    public BigDecimal umbralAplicable(ParametrosAfiliado parametros, Canal canal) {
        BigDecimal umbralCanal = umbralRevisionPorCanal.get(canal);
        if (umbralCanal != null) {
            return umbralCanal;
        }
        return parametros.umbralRevision() != null ? parametros.umbralRevision() : umbralRevisionPorDefecto;
    }

    public void validarTopeMensual(BigDecimal acumuladoMes, BigDecimal monto, BigDecimal topeMensual, String periodo) {
        BigDecimal nuevoTotal = acumuladoMes.add(monto);
        if (nuevoTotal.compareTo(topeMensual) > 0) {
            BigDecimal disponible = topeMensual.subtract(acumuladoMes).max(BigDecimal.ZERO);
            throw new ReglaNegocioException("TOPE_MENSUAL_EXCEDIDO",
                    "El aporte supera el tope mensual del afiliado de " + formatear(topeMensual)
                            + ". Disponible para " + periodo + ": " + formatear(disponible));
        }
    }

    public boolean requiereRevision(BigDecimal monto, BigDecimal umbralRevision) {
        return monto.compareTo(umbralRevision) > 0;
    }

    private static String formatear(BigDecimal valor) {
        NumberFormat formato = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-CO"));
        formato.setMinimumFractionDigits(2);
        formato.setMaximumFractionDigits(2);
        return "$" + formato.format(valor);
    }
}
