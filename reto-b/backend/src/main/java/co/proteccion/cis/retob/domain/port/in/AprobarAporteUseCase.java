package co.proteccion.cis.retob.domain.port.in;

import co.proteccion.cis.retob.domain.model.Aporte;

/**
 * Puerto de entrada: resolución de aportes marcados para revisión.
 * Un revisor aprueba (pasa a contar para el tope) o rechaza un aporte pendiente.
 */
public interface AprobarAporteUseCase {

    /**
     * Aprueba un aporte pendiente. El cupo del tope ya fue reservado al registrarse,
     * por lo que aprobar no modifica el saldo y nunca puede superar el tope.
     *
     * @throws co.proteccion.cis.retob.domain.model.ReglaNegocioException
     *         si el aporte no está en estado PENDIENTE_REVISION
     * @throws co.proteccion.cis.retob.domain.model.AporteNoEncontradoException
     *         si no existe el aporte
     */
    Aporte aprobar(Long aporteId);

    /** Rechaza un aporte pendiente y libera la reserva que tomó del tope mensual. */
    Aporte rechazar(Long aporteId);
}
