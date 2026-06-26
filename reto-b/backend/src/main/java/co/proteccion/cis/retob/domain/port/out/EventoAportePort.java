package co.proteccion.cis.retob.domain.port.out;

/**
 * Puerto de salida: registra eventos de dominio del ciclo de vida de un aporte
 * (trazabilidad / auditoría). La implementación persiste en {@code evento_aporte}.
 */
public interface EventoAportePort {

    /** Tipos de evento soportados. */
    enum Tipo {
        APORTE_REGISTRADO,
        APORTE_MARCADO_REVISION,
        APORTE_APROBADO,
        APORTE_RECHAZADO
    }

    void registrar(Long aporteId, Tipo tipo);
}
