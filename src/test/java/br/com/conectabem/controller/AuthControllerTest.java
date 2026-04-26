package br.com.conectabem.controller;

import br.com.conectabem.dto.user.LoginRequest;
import br.com.conectabem.dto.user.LoginResponse;
import br.com.conectabem.dto.user.RegisterRequest;
import br.com.conectabem.model.Gender;
import br.com.conectabem.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Test
    void registerReturnsJwtInResponse() {
        AuthController controller = new AuthController(authService);
        RegisterRequest request = createRegisterRequest();

        when(authService.register(request)).thenReturn("jwt");

        LoginResponse response = controller.register(request);

        assertEquals("jwt", response.jwtToken());
    }

    @Test
    void loginReturnsJwtInResponse() {
        AuthController controller = new AuthController(authService);
        LoginRequest request = new LoginRequest("u", "senha123");

        when(authService.login(request)).thenReturn("jwt");

        LoginResponse response = controller.login(request);

        assertEquals("jwt", response.jwtToken());
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

