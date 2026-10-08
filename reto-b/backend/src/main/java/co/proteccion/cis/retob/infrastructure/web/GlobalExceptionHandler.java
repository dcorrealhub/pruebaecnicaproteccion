package co.proteccion.cis.retob.infrastructure.web;

import co.proteccion.cis.retob.domain.exception.ConflictoConcurrenciaException;
import co.proteccion.cis.retob.domain.exception.ConflictoIdempotenciaException;
import co.proteccion.cis.retob.domain.exception.DominioException;
import co.proteccion.cis.retob.domain.exception.ReglaNegocioException;
import co.proteccion.cis.retob.domain.exception.SolicitudInvalidaException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

/**
 * Traduce excepciones a respuestas RFC 7807 (ProblemDetail) con un {@code codigo} estable.
 * Nunca expone trazas ni mensajes internos (OWASP A05/A09): los errores no controlados
 * se registran en el log y el cliente recibe un mensaje genérico.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ReglaNegocioException.class)
    public ProblemDetail reglaNegocio(ReglaNegocioException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, ex, "Aporte rechazado");
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ProblemDetail solicitudInvalida(SolicitudInvalidaException ex) {
        return problema(HttpStatus.BAD_REQUEST, ex, "Solicitud inválida");
    }

    @ExceptionHandler(ConflictoIdempotenciaException.class)
    public ProblemDetail conflictoIdempotencia(ConflictoIdempotenciaException ex) {
        return problema(HttpStatus.CONFLICT, ex, "Clave de idempotencia reutilizada");
    }

    @ExceptionHandler(ConflictoConcurrenciaException.class)
    public ResponseEntity<ProblemDetail> conflictoConcurrencia(ConflictoConcurrenciaException ex) {
        log.warn("Conflicto de concurrencia al registrar aporte: {}", ex.getCause() != null
                ? ex.getCause().getClass().getSimpleName() : "-");
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .header(HttpHeaders.RETRY_AFTER, "1")
                .body(problema(HttpStatus.CONFLICT, ex, "Operación concurrente"));
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail errorNoControlado(Exception ex) {
        log.error("Error no controlado", ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado. Intente más tarde");
        pd.setTitle("Error interno");
        pd.setProperty("codigo", "ERROR_INTERNO");
        return pd;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        List<String> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest().body(validacion(errores));
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                            HttpHeaders headers,
                                                                            HttpStatusCode status,
                                                                            WebRequest request) {
        List<String> errores = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream())
                .map(MessageSourceResolvable::getDefaultMessage)
                .toList();
        return ResponseEntity.badRequest().body(validacion(errores));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "El cuerpo de la solicitud no es un JSON válido o tiene tipos incorrectos");
        pd.setTitle("Solicitud inválida");
        pd.setProperty("codigo", "CUERPO_INVALIDO");
        return ResponseEntity.badRequest().body(pd);
    }

    private static ProblemDetail validacion(List<String> errores) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, String.join("; ", errores));
        pd.setTitle("Datos inválidos");
        pd.setProperty("codigo", "VALIDACION");
        pd.setProperty("errores", errores);
        return pd;
    }

    private static ProblemDetail problema(HttpStatus status, DominioException ex, String titulo) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        pd.setTitle(titulo);
        pd.setProperty("codigo", ex.getCodigo());
        return pd;
    }
}
