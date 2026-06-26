package co.proteccion.cis.retob.infrastructure.web;

import co.proteccion.cis.retob.domain.model.AporteNoEncontradoException;
import co.proteccion.cis.retob.domain.model.ReglaNegocioException;
import co.proteccion.cis.retob.infrastructure.web.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce las excepciones de negocio y de validación a respuestas HTTP claras.
 *
 * <ul>
 *   <li>{@link MethodArgumentNotValidException} / {@link IllegalArgumentException} → 400</li>
 *   <li>{@link ReglaNegocioException} (tope excedido, transición inválida) → 422</li>
 *   <li>{@link AporteNoEncontradoException} → 404</li>
 *   <li>Cualquier otra → 500</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(ErrorResponse.ofValidacion(
                HttpStatus.BAD_REQUEST.value(), "VALIDACION", "Datos de entrada inválidos", errores));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(), "VALIDACION", ex.getMessage()));
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponse> handleReglaNegocio(ReglaNegocioException ex) {
        return ResponseEntity.unprocessableEntity().body(ErrorResponse.of(
                HttpStatus.UNPROCESSABLE_ENTITY.value(), "REGLA_NEGOCIO", ex.getMessage()));
    }

    @ExceptionHandler(AporteNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleNoEncontrado(AporteNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(), "NO_ENCONTRADO", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenerico(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(), "ERROR_INTERNO",
                "Ocurrió un error inesperado procesando la solicitud"));
    }
}
