package co.proteccion.cis.retob.infrastructure.persistence.adapter;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Identifica qué restricción de base de datos causó una {@link DataIntegrityViolationException},
 * para traducir a conflicto de concurrencia solo las violaciones esperadas.
 */
final class ViolacionRestriccion {

    private ViolacionRestriccion() {}

    static boolean es(DataIntegrityViolationException e, String restriccion) {
        return e.getCause() instanceof ConstraintViolationException cve
                && restriccion.equalsIgnoreCase(cve.getConstraintName());
    }
}
