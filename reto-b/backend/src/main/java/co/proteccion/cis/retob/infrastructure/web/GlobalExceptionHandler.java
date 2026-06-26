package co.proteccion.cis.retob.infrastructure.web;

import co.proteccion.cis.retob.domain.exception.AfiliadoNotFoundException;
import co.proteccion.cis.retob.domain.exception.AporteNotFoundException;
import co.proteccion.cis.retob.domain.exception.TopeMensualExcedidoException;
import co.proteccion.cis.retob.domain.exception.TransicionEstadoInvalidaException;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Negocio ─────────────────────────────────────────────────────────────

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

    // ── Concurrencia ─────────────────────────────────────────────────────────

    @ExceptionHandler({OptimisticLockException.class, ObjectOptimisticLockingFailureException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleOptimisticLock(Exception ex) {
        return errorBody("CONFLICTO_CONCURRENCIA",
                "El registro fue modificado por otra operación concurrente. Reintente la solicitud.");
    }

    /**
     * DataIntegrityViolationException cubre:
     *  - Violación de UNIQUE constraint (afiliadoId duplicado, idempotenciaKey duplicado,
     *    saldo_mensual (afiliado_id, mes) por race condition en inicializar)
     *  - Violación de NOT NULL o FK
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleDataIntegrity(DataIntegrityViolationException ex) {
        return errorBody("DUPLICADO",
                "Ya existe un registro con los mismos datos únicos. Verifique los campos e intente de nuevo.");
    }

    // ── Validación de entrada ─────────────────────────────────────────────────

    /** Body JSON inválido: campos @Valid en records (@RequestBody). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleBodyValidation(MethodArgumentNotValidException ex) {
        var campos = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (a, b) -> a));
        return Map.of(
                "error", "VALIDACION_FALLIDA",
                "mensaje", "Uno o más campos del cuerpo no son válidos",
                "campos", campos
        );
    }

    /** @RequestParam / @PathVariable con @Pattern, @NotBlank, etc. validados por @Validated. */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleParamValidation(ConstraintViolationException ex) {
        var campos = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> {
                            String path = v.getPropertyPath().toString();
                            // quita el prefijo "methodName." que Spring agrega
                            int dot = path.lastIndexOf('.');
                            return dot >= 0 ? path.substring(dot + 1) : path;
                        },
                        v -> v.getMessage(),
                        (a, b) -> a
                ));
        return Map.of(
                "error", "VALIDACION_FALLIDA",
                "mensaje", "Uno o más parámetros de la solicitud no son válidos",
                "campos", campos
        );
    }

    /** Enum o tipo inválido en el body JSON (ej: canal="INVALIDO"). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        return errorBody("FORMATO_INVALIDO",
                "El cuerpo de la solicitud contiene un valor inválido o un formato incorrecto. " +
                "Verifique los enums (canal, nuevoEstado) y los tipos de datos.");
    }

    /** Tipo incompatible en @PathVariable o @RequestParam (ej: /aportes/abc/estado). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Class<?> tipo = ex.getRequiredType();
        String nombreTipo = tipo != null ? tipo.getSimpleName() : "desconocido";
        return errorBody("TIPO_INVALIDO",
                String.format("El parámetro '%s' debe ser de tipo %s.", ex.getName(), nombreTipo));
    }

    // ── Utilidad ──────────────────────────────────────────────────────────────

    private Map<String, String> errorBody(String codigo, String mensaje) {
        return Map.of("error", codigo, "mensaje", mensaje);
    }
}
