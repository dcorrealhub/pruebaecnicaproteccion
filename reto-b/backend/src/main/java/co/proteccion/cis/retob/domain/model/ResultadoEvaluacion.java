package co.proteccion.cis.retob.domain.model;

/**
 * Resultado de evaluar un aporte contra las reglas de negocio.
 *
 * @param marcadaRevision true si el aporte debe quedar marcado para revisión posterior
 */
public record ResultadoEvaluacion(boolean marcadaRevision) {}
