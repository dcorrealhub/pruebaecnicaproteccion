package co.proteccion.cis.retob.domain.exception;

public class ConcurrenciaSaldoException extends DomainException {

    public ConcurrenciaSaldoException(String afiliadoId, String periodo) {
        super("CONCURRENCIA_SALDO",
                "Conflicto de concurrencia al actualizar saldo de %s en periodo %s".formatted(afiliadoId, periodo));
    }
}
