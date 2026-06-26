package co.proteccion.cis.retob.domain.model;

/**
 * Estado del ciclo de vida de un aporte.
 *
 * <p>Regla de negocio clave: solo los aportes {@link #APROBADO} cuentan para el
 * tope mensual del afiliado. Un aporte que supera el umbral de revisión queda
 * {@link #PENDIENTE_REVISION} y no afecta el saldo hasta ser aprobado.
 */
public enum EstadoAporte {

    /** Aporte válido que cuenta para el acumulado (tope) mensual. */
    APROBADO,

    /** Superó el umbral de revisión; espera aprobación manual y no cuenta para el tope. */
    PENDIENTE_REVISION,

    /** Rechazado por un revisor; no cuenta para el tope. */
    RECHAZADO;

    public boolean cuentaParaTope() {
        return this == APROBADO;
    }
}
