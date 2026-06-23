package co.proteccion.cis.retob.infrastructure.web.exception;

import co.proteccion.cis.retob.domain.exception.CanalInvalidoException;
import co.proteccion.cis.retob.domain.exception.ConcurrenciaSaldoException;
import co.proteccion.cis.retob.domain.exception.DomainException;
import co.proteccion.cis.retob.domain.exception.IdempotenciaEnProcesoException;
import co.proteccion.cis.retob.domain.exception.MontoInvalidoException;
import co.proteccion.cis.retob.domain.exception.PeriodoInvalidoException;
import co.proteccion.cis.retob.domain.exception.TopeMensualExcedidoException;
import co.proteccion.cis.retob.infrastructure.web.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MontoInvalidoException.class)
    public ResponseEntity<ErrorResponse> handleMontoInvalido(MontoInvalidoException ex) {
        return toResponse(ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(CanalInvalidoException.class)
    public ResponseEntity<ErrorResponse> handleCanalInvalido(CanalInvalidoException ex) {
        return toResponse(ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(PeriodoInvalidoException.class)
    public ResponseEntity<ErrorResponse> handlePeriodoInvalido(PeriodoInvalidoException ex) {
        return toResponse(ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TopeMensualExcedidoException.class)
    public ResponseEntity<ErrorResponse> handleTopeMensual(TopeMensualExcedidoException ex) {
        return toResponse(ex, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(ConcurrenciaSaldoException.class)
    public ResponseEntity<ErrorResponse> handleConcurrencia(ConcurrenciaSaldoException ex) {
        return toResponse(ex, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(IdempotenciaEnProcesoException.class)
    public ResponseEntity<ErrorResponse> handleIdempotenciaEnProceso(IdempotenciaEnProcesoException ex) {
        return toResponse(ex, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("VALIDACION", ex.getMessage()));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomain(DomainException ex) {
        return toResponse(ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Solicitud invalida");
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("VALIDACION", mensaje));
    }

    private ResponseEntity<ErrorResponse> toResponse(DomainException ex, HttpStatus status) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(ex.getCodigo(), ex.getMessage()));
    }
}
