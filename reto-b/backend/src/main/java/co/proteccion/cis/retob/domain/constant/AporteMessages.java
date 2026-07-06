package co.proteccion.cis.retob.domain.constant;

public final class AporteMessages {

    public static final String AFILIADO_REQUERIDO = "El afiliadoId es obligatorio";
    public static final String CANAL_REQUERIDO = "El canal es obligatorio";
    public static final String IDEMPOTENCIA_REQUERIDA = "La clave de idempotencia es obligatoria";
    public static final String MONTO_POSITIVO = "El monto debe ser positivo";

    public static final String NOT_FOUND = "No existe un aporte con el id indicado";

    public static final String TOPE_EXCEDIDO_FMT =
            "El aporte supera el tope mensual del afiliado (tope: %s, acumulado resultante: %s)";
    public static final String SOLO_PENDIENTE_APROBAR =
            "Solo se puede aprobar un aporte en estado PENDIENTE_REVISION";
    public static final String SOLO_PENDIENTE_RECHAZAR =
            "Solo se puede rechazar un aporte en estado PENDIENTE_REVISION";
    public static final String CONCURRENCIA =
            "No se pudo procesar el aporte por alta concurrencia sobre el saldo mensual; reintente.";

    private AporteMessages() {
    }
}
