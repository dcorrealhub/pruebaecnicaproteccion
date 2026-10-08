package co.proteccion.cis.retob.domain.model;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Parámetros de negocio aplicables a un aporte: tope mensual y umbral de revisión.
 *
 * <p>El umbral de revisión depende del canal: hay un umbral por defecto y, opcionalmente,
 * overrides por canal (p. ej. SUCURSAL tiene un umbral más bajo por mayor riesgo). El tope
 * mensual es único.</p>
 *
 * <p>La resolución se hace por {@code afiliadoId} + {@code mes} (ver {@code ParametrosAportePort}),
 * de modo que extender a parámetros por afiliado sea solo cambiar la fuente que los produce,
 * sin tocar el dominio.</p>
 *
 * @param topeMensual             máximo acumulado permitido en el mes (inclusive)
 * @param umbralRevisionDefault   umbral por defecto a partir del cual (exclusivo) se marca para revisión
 * @param umbralRevisionPorCanal  overrides de umbral por canal (puede estar vacío)
 */
public record ParametrosAporte(BigDecimal topeMensual,
                               BigDecimal umbralRevisionDefault,
                               Map<Canal, BigDecimal> umbralRevisionPorCanal) {

    public ParametrosAporte {
        if (topeMensual == null || umbralRevisionDefault == null) {
            throw new IllegalArgumentException("topeMensual y umbralRevisionDefault son obligatorios");
        }
        umbralRevisionPorCanal = umbralRevisionPorCanal == null
                ? Map.of()
                : Map.copyOf(umbralRevisionPorCanal);
    }

    /** Umbral de revisión aplicable al canal dado: el override del canal si existe, o el default. */
    public BigDecimal umbralRevisionPara(Canal canal) {
        return umbralRevisionPorCanal.getOrDefault(canal, umbralRevisionDefault);
    }
}
