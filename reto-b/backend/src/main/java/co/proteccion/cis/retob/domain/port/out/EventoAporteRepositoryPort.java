package co.proteccion.cis.retob.domain.port.out;

import co.proteccion.cis.retob.domain.model.EventoAporte;

/**
 * Puerto de salida: abstracción de persistencia para eventos de aporte.
 * La implementación vive en la capa de infraestructura.
 */
public interface EventoAporteRepositoryPort {

    EventoAporte guardar(EventoAporte evento);
}
