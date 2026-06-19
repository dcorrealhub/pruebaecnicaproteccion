package co.proteccion.cis.retob.infrastructure.web;

import co.proteccion.cis.retob.domain.exception.ReglaNegocioException;
import jakarta.persistence.OptimisticLockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ReglaNegocioException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleReglaNegocio(ReglaNegocioException ex) {
        log.warn("Regla de negocio violada — codigo={}, mensaje={}", ex.getCodigo(), ex.getMessage());
        return new ErrorResponse(Instant.now(), ex.getCodigo(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidacion(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("Validacion fallida — {}", detalle);
        return new ErrorResponse(Instant.now(), "VALIDACION_FALLIDA", detalle);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleHeaderFaltante(MissingRequestHeaderException ex) {
        String mensaje = "Header requerido ausente: " + ex.getHeaderName();
        log.warn(mensaje);
        return new ErrorResponse(Instant.now(), "HEADER_REQUERIDO_AUSENTE", mensaje);
    }

    @ExceptionHandler({OptimisticLockException.class, ObjectOptimisticLockingFailureException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConcurrencia(RuntimeException ex) {
        log.warn("Conflicto de concurrencia detectado — {}", ex.getMessage());
        return new ErrorResponse(Instant.now(), "CONFLICTO_CONCURRENCIA",
                "La operación no pudo completarse por conflicto de concurrencia. Por favor reintente.");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGenerico(Exception ex) {
        log.error("Error inesperado", ex);
        return new ErrorResponse(Instant.now(), "ERROR_INTERNO",
                "Ocurrió un error inesperado. Contacte al equipo de soporte.");
    }

    public record ErrorResponse(Instant timestamp, String codigo, String mensaje) {}
}
