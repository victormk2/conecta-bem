package br.com.conectabem.controller;

import br.com.conectabem.dto.user.ForgotPasswordRequest;
import br.com.conectabem.dto.user.LoginRequest;
import br.com.conectabem.dto.user.LoginResponse;
import br.com.conectabem.dto.user.RegisterRequest;
import br.com.conectabem.model.Gender;
import br.com.conectabem.service.AuthService;
import br.com.conectabem.service.PasswordResetService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;
    @Mock
    private PasswordResetService passwordResetService;

    @Test
    void registerReturnsJwtInResponse() {
        AuthController controller = new AuthController(authService, passwordResetService);
        RegisterRequest request = createRegisterRequest();

        when(authService.register(request)).thenReturn("jwt");

        LoginResponse response = controller.register(request);

        assertEquals("jwt", response.jwtToken());
    }

    @Test
    void loginReturnsJwtInResponse() {
        AuthController controller = new AuthController(authService, passwordResetService);
        LoginRequest request = new LoginRequest("u", "senha123");

        when(authService.login(request)).thenReturn("jwt");

        LoginResponse response = controller.login(request);

        assertEquals("jwt", response.jwtToken());
    }

    @Nested
    class ForgotPasswordTest {

        @Test
        void shouldReturn200Regardless() {
            var controller = new AuthController(authService, passwordResetService);
            var request = new ForgotPasswordRequest("any@email.com");

            var response = controller.forgotPassword(request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        void shouldDelegateToPasswordResetService() {
            var controller = new AuthController(authService, passwordResetService);
            var request = new ForgotPasswordRequest("user@test.com");

            controller.forgotPassword(request);

            verify(passwordResetService).forgotPassword("user@test.com");
        }

        @Test
        void shouldReturn200EvenWhenEmailDoesNotExist() {
            var controller = new AuthController(authService, passwordResetService);
            var request = new ForgotPasswordRequest("unknown@test.com");

            doNothing().when(passwordResetService).forgotPassword(anyString());

            var response = controller.forgotPassword(request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    RegisterRequest createRegisterRequest(){
        return new RegisterRequest(
                "u",
                "senha123",
                "u@e.com",
                "User",
                "12345678901",
                LocalDate.of(2000, 1, 15),
                Gender.MALE,
                "47999990000",
                "userinstagram",
                "https://linkedin.com/in/u"
        );
    }
}

