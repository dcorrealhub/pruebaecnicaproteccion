package co.proteccion.cis.retob.domain.model.aporte.gateway;

/**
 * Puerto de trazabilidad del ciclo de vida de un aporte (auditoría).
 */
public interface EventoOutputPort {

    enum Tipo {
        APORTE_REGISTRADO,
        APORTE_MARCADO_REVISION,
        APORTE_APROBADO,
        APORTE_RECHAZADO
    }

    void registrar(Long aporteId, Tipo tipo);
}
