package co.proteccion.cis.retob.infrastructure.entrypoint.constant;

/**
 * Códigos de error y mensajes genéricos de la capa REST.
 */
public final class ApiMessages {

    // Códigos de error (campo "error" del ErrorResponse)
    public static final String CODE_VALIDACION = "VALIDACION";
    public static final String CODE_REGLA_NEGOCIO = "REGLA_NEGOCIO";
    public static final String CODE_NO_ENCONTRADO = "NO_ENCONTRADO";
    public static final String CODE_ERROR_INTERNO = "ERROR_INTERNO";

    // Mensajes genéricos
    public static final String VALIDACION_INVALIDA = "Datos de entrada inválidos";
    public static final String ERROR_INTERNO = "Ocurrió un error inesperado procesando la solicitud";

    private ApiMessages() {
    }
}
