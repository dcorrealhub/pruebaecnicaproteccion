package co.proteccion.cis.retob.domain.port.out;

import co.proteccion.cis.retob.domain.model.Aporte;

/**
 * Puerto de salida: registro de eventos de trazabilidad de aportes.
 * No contiene datos sensibles más allá del identificador del aporte.
 */
public interface EventoAportePort {

    /**
     * Registra el evento de que un aporte fue creado (tipo APORTE_REGISTRADO).
     *
     * @param aporte aporte ya persistido (con id asignado)
     */
    void registrarCreado(Aporte aporte);
}
