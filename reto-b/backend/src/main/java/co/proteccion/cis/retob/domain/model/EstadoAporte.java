package co.proteccion.cis.retob.domain.model;

import co.proteccion.cis.retob.domain.exception.TransicionEstadoInvalidaException;

import java.util.Set;
import java.util.Map;

public enum EstadoAporte {

    PENDIENTE,
    EN_REVISION,
    APROBADO,
    RECHAZADO;

    private static final Map<EstadoAporte, Set<EstadoAporte>> TRANSICIONES_VALIDAS = Map.of(
            PENDIENTE,    Set.of(EN_REVISION, APROBADO),
            EN_REVISION,  Set.of(APROBADO, RECHAZADO),
            APROBADO,     Set.of(),
            RECHAZADO,    Set.of()
    );

    public EstadoAporte transicionar(EstadoAporte nuevo) {
        if (!TRANSICIONES_VALIDAS.get(this).contains(nuevo)) {
            throw new TransicionEstadoInvalidaException(this, nuevo);
        }
        return nuevo;
    }
}
