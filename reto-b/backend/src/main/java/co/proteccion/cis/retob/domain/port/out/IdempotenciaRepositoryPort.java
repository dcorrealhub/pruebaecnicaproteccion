package co.proteccion.cis.retob.domain.port.out;

import java.util.Optional;

/**
 * Puerto de salida: registro atómico de claves de idempotencia.
 */
public interface IdempotenciaRepositoryPort {

    enum Estado {
        EN_PROCESO,
        COMPLETADO
    }

    record Registro(String idempotenciaKey, Long aporteId, Estado estado) {}

    Optional<Registro> findByKey(String idempotenciaKey);

    /**
     * Intenta reservar la clave (INSERT con UNIQUE). Retorna true si este hilo ganó el claim.
     */
    boolean intentarClaim(String idempotenciaKey);

    void completar(String idempotenciaKey, Long aporteId);

    /**
     * Libera un claim fallido para permitir reintentos con la misma clave.
     */
    void liberarClaim(String idempotenciaKey);
}
