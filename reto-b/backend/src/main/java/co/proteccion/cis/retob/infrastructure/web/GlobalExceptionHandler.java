package co.proteccion.cis.retob.infrastructure.web;

import co.proteccion.cis.retob.domain.exception.ReglaNegocioException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
class GlobalExceptionHandler {

    record ErrorResponse(String mensaje) {}

    @ExceptionHandler(ReglaNegocioException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    ErrorResponse handleRegla(ReglaNegocioException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, String> handleValidacion(MethodArgumentNotValidException ex) {
        return ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage,
                        (a, b) -> a));
    }

    // Violaciones de @Validated en @RequestParam / @PathVariable
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, String> handleConstraintViolation(ConstraintViolationException ex) {
        return ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> {
                            String path = v.getPropertyPath().toString();
                            int dot = path.lastIndexOf('.');
                            return dot >= 0 ? path.substring(dot + 1) : path;
                        },
                        v -> v.getMessage(),
                        (a, b) -> a));
    }

    // Conflicto de concurrencia optimista — el cliente puede reintentar
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ErrorResponse handleConflicto(ObjectOptimisticLockingFailureException ex) {
        return new ErrorResponse("Conflicto de concurrencia al actualizar el saldo. Reintente la operación.");
    }

    // Violación de restricción única en BD (ej: idempotencia_key duplicada por race condition)
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ErrorResponse handleIntegridad(DataIntegrityViolationException ex) {
        return new ErrorResponse("Conflicto de datos: la operación viola una restricción de unicidad. Reintente.");
    }
}
