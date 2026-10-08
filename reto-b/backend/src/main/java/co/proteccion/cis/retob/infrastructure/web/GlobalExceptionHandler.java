package co.proteccion.cis.retob.infrastructure.web;

import co.proteccion.cis.retob.domain.exception.ConflictoConcurrenciaException;
import co.proteccion.cis.retob.domain.exception.RecursoNoEncontradoException;
import co.proteccion.cis.retob.domain.exception.ReglaNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.List;

/**
 * Manejo centralizado de errores con contrato {@link ProblemDetail} (RFC 7807).
 *
 * <p>No se hace eco del input del usuario ni se filtran detalles internos (stacktrace,
 * mensajes de SQL o de infraestructura). Los mensajes son claros y accionables.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String BASE_TYPE = "https://api.proteccion.co/problems/";

    /** 422 — violación de una regla de negocio del dominio (monto, tope, fecha). */
    @ExceptionHandler(ReglaNegocioException.class)
    public ProblemDetail manejarReglaNegocio(ReglaNegocioException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "regla-negocio",
                "Regla de negocio no cumplida", ex.getMessage());
    }

    /** 404 — recurso inexistente. */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail manejarNoEncontrado(RecursoNoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, "no-encontrado",
                "Recurso no encontrado", ex.getMessage());
    }

    /** 409 — conflicto de concurrencia tras agotar los reintentos. */
    @ExceptionHandler(ConflictoConcurrenciaException.class)
    public ProblemDetail manejarConflicto(ConflictoConcurrenciaException ex) {
        return problema(HttpStatus.CONFLICT, "conflicto-concurrencia",
                "Conflicto de concurrencia", ex.getMessage());
    }

    /** 400 — fallos de Bean Validation en el cuerpo de la petición. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail manejarValidacion(MethodArgumentNotValidException ex) {
        List<String> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(this::describirCampo)
                .toList();
        ProblemDetail pd = problema(HttpStatus.BAD_REQUEST, "validacion",
                "Datos de entrada inválidos", "Uno o más campos no son válidos.");
        pd.setProperty("errores", errores);
        return pd;
    }

    /** 400 — JSON mal formado o valor de enum/tipo no convertible (p. ej. canal inexistente). */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ProblemDetail manejarCuerpoIlegible() {
        return problema(HttpStatus.BAD_REQUEST, "formato",
                "Petición mal formada",
                "El cuerpo o los parámetros de la petición no tienen el formato esperado.");
    }

    /** 500 — fallback: no se filtra el detalle interno al cliente, pero sí se registra. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail manejarInesperado(Exception ex, HttpServletRequest req) {
        log.error("Error inesperado procesando {} {}", req.getMethod(), req.getRequestURI(), ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "interno",
                "Error interno", "Ocurrió un error procesando la solicitud.");
    }

    private String describirCampo(FieldError fe) {
        return fe.getField() + ": " + fe.getDefaultMessage();
    }

    private ProblemDetail problema(HttpStatus status, String tipo, String titulo, String detalle) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detalle);
        pd.setTitle(titulo);
        pd.setType(URI.create(BASE_TYPE + tipo));
        return pd;
    }
}
