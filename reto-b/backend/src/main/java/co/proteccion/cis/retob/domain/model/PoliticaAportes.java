package co.proteccion.cis.retob.domain.model;

import co.proteccion.cis.retob.domain.exception.MontoInvalidoException;
import co.proteccion.cis.retob.domain.exception.TopeMensualExcedidoException;

import java.math.BigDecimal;

/**
 * Reglas de negocio de los aportes voluntarios, aisladas en el dominio.
 *
 * <p>Clase pura: sin Spring, sin persistencia. Los parámetros ({@link ParametrosAporte})
 * se reciben por invocación, de modo que su origen (global o por afiliado) sea
 * indiferente para esta lógica.</p>
 *
 * <p>Reglas:</p>
 * <ol>
 *   <li>El monto debe ser positivo (&gt; 0).</li>
 *   <li>El acumulado del mes + el monto no puede superar el tope mensual (comparación con compareTo).</li>
 *   <li>Si el monto supera el umbral de revisión aplicable al canal, el aporte se marca para
 *       revisión (no se rechaza). El umbral puede variar por canal (p. ej. SUCURSAL más bajo).</li>
 * </ol>
 */
public final class PoliticaAportes {

    private PoliticaAportes() {
    }

    /**
     * Evalúa un aporte contra las reglas de negocio.
     *
     * @param monto          monto del aporte (debe ser positivo)
     * @param canal          canal de origen (determina el umbral de revisión aplicable)
     * @param acumuladoMes   total ya aportado por el afiliado en el mes (nunca null; usar ZERO si no hay saldo)
     * @param parametros     tope mensual y umbral(es) de revisión aplicables
     * @return resultado con la marca de revisión
     * @throws MontoInvalidoException       si el monto no es positivo
     * @throws TopeMensualExcedidoException si el acumulado + monto supera el tope
     */
    public static ResultadoEvaluacion evaluar(BigDecimal monto,
                                              Canal canal,
                                              BigDecimal acumuladoMes,
                                              ParametrosAporte parametros) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new MontoInvalidoException("El monto del aporte debe ser mayor a cero.");
        }

        BigDecimal acumulado = acumuladoMes == null ? BigDecimal.ZERO : acumuladoMes;
        BigDecimal nuevoAcumulado = acumulado.add(monto);

        if (nuevoAcumulado.compareTo(parametros.topeMensual()) > 0) {
            throw new TopeMensualExcedidoException(
                    "El aporte supera el tope mensual permitido para el afiliado.");
        }

        boolean marcadaRevision = monto.compareTo(parametros.umbralRevisionPara(canal)) > 0;
        return new ResultadoEvaluacion(marcadaRevision);
    }
}
