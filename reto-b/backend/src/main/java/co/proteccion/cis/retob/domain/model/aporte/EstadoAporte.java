package co.proteccion.cis.retob.domain.model.aporte;

/**
 * Estado del ciclo de vida de un aporte.
 *
 * <p>Modelo de reserva: tanto {@link #APROBADO} como {@link #PENDIENTE_REVISION} reservan
 * cupo del tope mensual; {@link #RECHAZADO} libera la reserva.
 */
public enum EstadoAporte {
    APROBADO,
    PENDIENTE_REVISION,
    RECHAZADO
}
