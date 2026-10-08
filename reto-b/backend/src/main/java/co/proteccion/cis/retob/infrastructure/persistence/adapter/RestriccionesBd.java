package co.proteccion.cis.retob.infrastructure.persistence.adapter;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Identifica qué restricción de base de datos causó una violación de integridad, para
 * traducir solo las esperadas (carreras de concurrencia) y no ocultar otros errores.
 */
final class RestriccionesBd {

    static final String UQ_APORTE_IDEMPOTENCIA = "uq_aporte_idempotencia";
    static final String UQ_SALDO_AFILIADO_MES = "uq_saldo_afiliado_mes";

    private RestriccionesBd() {}

    static boolean violoRestriccion(DataIntegrityViolationException e, String nombre) {
        Throwable causa = e;
        while (causa != null) {
            if (causa instanceof ConstraintViolationException cve
                    && cve.getConstraintName() != null
                    && cve.getConstraintName().toLowerCase().contains(nombre)) {
                return true;
            }
            causa = causa.getCause();
        }
        return false;
    }
}
