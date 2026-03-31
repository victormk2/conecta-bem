package br.com.conectabem.controller;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void handleIllegalArgumentReturnsUnprocessableEntity() {
        ResponseEntity<?> response = handler.handleIllegalArgument(new IllegalArgumentException("bad request"));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertEquals(Map.of("error", "bad request"), response.getBody());
    }

    @Test
    void handleDataIntegrityReturnsFixedMessage() {
        ResponseEntity<?> response = handler.handleDataIntegrity(new DataIntegrityViolationException("db"));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertEquals(Map.of("error", "data integrity violation"), response.getBody());
    }

    @Test
    void handleValidationReturnsFieldMap() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "must not be blank"));
        bindingResult.addError(new FieldError("request", "password", "size must be between 8 and 2147483647"));

        Method method = SampleController.class.getDeclaredMethod("sample", SampleRequest.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<?> response = handler.handleValidation(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(Map.of(
                "email", "must not be blank",
                "password", "size must be between 8 and 2147483647"
        ), response.getBody());
    }

    private static final class SampleController {
        @SuppressWarnings("unused")
        void sample(SampleRequest request) {
        }
    }

    private static final class SampleRequest {
    }
}
