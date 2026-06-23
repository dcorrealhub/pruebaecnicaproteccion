package co.proteccion.cis.retob.domain.exception;

public class IdempotenciaEnProcesoException extends DomainException {

    public IdempotenciaEnProcesoException(String idempotenciaKey) {
        super(
                "IDEMPOTENCIA_EN_PROCESO",
                "La solicitud con clave de idempotencia '%s' ya está en procesamiento".formatted(idempotenciaKey)
        );
    }
}
