package co.proteccion.cis.retob.infrastructure.web;

import co.proteccion.cis.retob.domain.exception.AporteRechazadoException;
import co.proteccion.cis.retob.domain.exception.ConcurrenciaConflictoException;
import co.proteccion.cis.retob.domain.exception.ConsultaInvalidaException;
import co.proteccion.cis.retob.domain.exception.IdempotenciaConflictoException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce excepciones a respuestas HTTP con formato {@link ProblemDetail} (RFC 7807).
 *
 * <ul>
 *   <li>400 — formato o estructura de la petición inválidos (con {@code errores} por campo).</li>
 *   <li>422 — la petición es válida pero viola una regla de negocio.</li>
 *   <li>409 — conflicto de concurrencia; el cliente puede reintentar con la misma clave.</li>
 *   <li>500 — error no esperado, sin exponer detalles internos.</li>
 * </ul>
 * Toda respuesta incluye la propiedad {@code codigo} para que el cliente distinga el motivo.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String SOLICITUD_INVALIDA = "SOLICITUD_INVALIDA";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacionBody(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errores.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return solicitudInvalida("La solicitud contiene campos inválidos", errores);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail validacionParametros(HandlerMethodValidationException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(resultado -> {
            String parametro = resultado.getMethodParameter().getParameterName();
            resultado.getResolvableErrors()
                    .forEach(error -> errores.putIfAbsent(parametro, error.getDefaultMessage()));
        });
        return solicitudInvalida("La solicitud contiene parámetros inválidos", errores);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail parametroFaltante(MissingServletRequestParameterException ex) {
        return solicitudInvalida("Falta un parámetro obligatorio",
                Map.of(ex.getParameterName(), "El parámetro es obligatorio"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail cuerpoIlegible(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof InvalidFormatException ife && !ife.getPath().isEmpty()) {
            String campo = ife.getPath().get(ife.getPath().size() - 1).getFieldName();
            return solicitudInvalida("La solicitud contiene campos inválidos",
                    Map.of(campo, mensajeFormatoInvalido(ife)));
        }
        return solicitudInvalida("El cuerpo de la solicitud no es un JSON válido", Map.of());
    }

    @ExceptionHandler(AporteRechazadoException.class)
    public ProblemDetail aporteRechazado(AporteRechazadoException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "Aporte rechazado", ex.getCodigo(), ex.getMessage());
    }

    @ExceptionHandler(IdempotenciaConflictoException.class)
    public ProblemDetail idempotenciaConflicto(IdempotenciaConflictoException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "Clave de idempotencia reutilizada", ex.getCodigo(), ex.getMessage());
    }

    @ExceptionHandler(ConsultaInvalidaException.class)
    public ProblemDetail consultaInvalida(ConsultaInvalidaException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "Consulta inválida", ex.getCodigo(), ex.getMessage());
    }

    @ExceptionHandler(ConcurrenciaConflictoException.class)
    public ProblemDetail concurrenciaConflicto(ConcurrenciaConflictoException ex) {
        log.warn("Conflicto de concurrencia: {}", ex.getMessage());
        return problema(HttpStatus.CONFLICT, "Conflicto de concurrencia", ex.getCodigo(), ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail errorInesperado(Exception ex) {
        log.error("Error no controlado", ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno", "ERROR_INTERNO",
                "Ocurrió un error inesperado. Intente nuevamente más tarde");
    }

    private String mensajeFormatoInvalido(InvalidFormatException ife) {
        Class<?> tipo = ife.getTargetType();
        if (tipo.isEnum()) {
            return "Valor '" + ife.getValue() + "' no permitido. Valores válidos: "
                    + Arrays.toString(tipo.getEnumConstants());
        }
        return "Valor '" + ife.getValue() + "' con formato inválido";
    }

    private ProblemDetail solicitudInvalida(String detalle, Map<String, String> errores) {
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Solicitud inválida", SOLICITUD_INVALIDA, detalle);
        problema.setProperty("errores", errores);
        return problema;
    }

    private ProblemDetail problema(HttpStatus status, String titulo, String codigo, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalle);
        problema.setTitle(titulo);
        problema.setProperty("codigo", codigo);
        return problema;
    }
}
