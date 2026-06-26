package co.proteccion.cis.retob.infrastructure.web;

import co.proteccion.cis.retob.domain.exception.AfiliadoNotFoundException;
import co.proteccion.cis.retob.domain.exception.AporteNotFoundException;
import co.proteccion.cis.retob.domain.exception.TopeMensualExcedidoException;
import co.proteccion.cis.retob.domain.exception.TransicionEstadoInvalidaException;
import jakarta.persistence.OptimisticLockException;
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
public class GlobalExceptionHandler {

    @ExceptionHandler(TopeMensualExcedidoException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Map<String, String> handleTopeMensual(TopeMensualExcedidoException ex) {
        return errorBody("TOPE_MENSUAL_EXCEDIDO", ex.getMessage());
    }

    @ExceptionHandler(TransicionEstadoInvalidaException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleTransicion(TransicionEstadoInvalidaException ex) {
        return errorBody("TRANSICION_INVALIDA", ex.getMessage());
    }

    @ExceptionHandler(AporteNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleAporteNotFound(AporteNotFoundException ex) {
        return errorBody("APORTE_NO_ENCONTRADO", ex.getMessage());
    }

    @ExceptionHandler(AfiliadoNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleAfiliadoNotFound(AfiliadoNotFoundException ex) {
        return errorBody("AFILIADO_NO_ENCONTRADO", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleIllegalArgument(IllegalArgumentException ex) {
        return errorBody("CONFLICTO", ex.getMessage());
    }

    @ExceptionHandler({OptimisticLockException.class, ObjectOptimisticLockingFailureException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleOptimisticLock(Exception ex) {
        return errorBody("CONFLICTO_CONCURRENCIA", "El registro fue modificado por otra operación. Reintente la solicitud.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidation(MethodArgumentNotValidException ex) {
        var campos = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (a, b) -> a));
        return Map.of(
                "error", "VALIDACION_FALLIDA",
                "mensaje", "Uno o más campos no son válidos",
                "campos", campos
        );
    }

    private Map<String, String> errorBody(String codigo, String mensaje) {
        return Map.of("error", codigo, "mensaje", mensaje);
    }
}
