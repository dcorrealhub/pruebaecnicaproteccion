package co.proteccion.cis.retob.infrastructure.entrypoint.rest;

import co.proteccion.cis.retob.domain.exception.DomainBusinessRuleException;
import co.proteccion.cis.retob.domain.exception.DomainNotFoundException;
import co.proteccion.cis.retob.domain.exception.DomainValidationException;
import co.proteccion.cis.retob.infrastructure.entrypoint.constant.ApiMessages;
import co.proteccion.cis.retob.infrastructure.entrypoint.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBeanValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError fe : exception.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(ErrorResponse.ofValidacion(
                HttpStatus.BAD_REQUEST.value(), ApiMessages.CODE_VALIDACION,
                ApiMessages.VALIDACION_INVALIDA, errores));
    }

    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<ErrorResponse> handleDomainValidation(DomainValidationException exception) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(), ApiMessages.CODE_VALIDACION, exception.getMessage()));
    }

    @ExceptionHandler(DomainNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(DomainNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(), ApiMessages.CODE_NO_ENCONTRADO, exception.getMessage()));
    }

    @ExceptionHandler(DomainBusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(DomainBusinessRuleException exception) {
        return ResponseEntity.unprocessableEntity().body(ErrorResponse.of(
                HttpStatus.UNPROCESSABLE_ENTITY.value(), ApiMessages.CODE_REGLA_NEGOCIO, exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception exception) {
        log.error("Error inesperado", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(), ApiMessages.CODE_ERROR_INTERNO, ApiMessages.ERROR_INTERNO));
    }
}
