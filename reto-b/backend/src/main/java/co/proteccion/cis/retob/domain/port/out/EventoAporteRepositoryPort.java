package co.proteccion.cis.retob.domain.port.out;

import co.proteccion.cis.retob.domain.model.TipoEventoAporte;

/**
 * Puerto de salida: registro de eventos de auditoría de aportes.
 * Se escribe en la misma transacción que el aporte, de modo que no puede
 * existir un aporte sin su evento ni un evento sin su aporte.
 */
public interface EventoAporteRepositoryPort {

    void registrar(Long aporteId, TipoEventoAporte tipo);
}
