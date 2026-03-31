package br.com.conectabem.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<?> handleDateTimeParse(DateTimeParseException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
            Map.of(
                "error", "invalid date format",
                "details", "expected format: yyyy-MM-ddTHH:mm:ss",
                "message", ex.getMessage()
            )
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleDataIntegrity(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("error", "data integrity violation"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe -> errors.put(fe.getField(), fe.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        String message = ex.getMessage();
        if (message.contains("Cannot deserialize value of type") || message.contains("from String")) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
                Map.of("error", "invalid data format", "details", extractErrorDetails(message))
            );
        }
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
            Map.of("error", "invalid request body format")
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception ex) {
        // Retorna 422 para erros genéricos ao invés de 500
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
            Map.of("error", "invalid request", "message", ex.getMessage())
        );
    }

    private static String extractErrorDetails(String message) {
        if (message.contains("java.time")) {
            return "invalid date format";
        }
        if (message.contains("java.lang")) {
            return "invalid data type";
        }
        return "invalid format";
    }
}
