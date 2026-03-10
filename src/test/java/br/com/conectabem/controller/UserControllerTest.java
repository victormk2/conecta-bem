package br.com.conectabem.controller;

import br.com.conectabem.dto.user.LoginRequest;
import br.com.conectabem.dto.user.LoginResponse;
import br.com.conectabem.dto.user.RegisterRequest;
import br.com.conectabem.dto.user.RegisterResponse;
import br.com.conectabem.model.User;
import br.com.conectabem.model.UserRole;
import br.com.conectabem.security.JwtUtil;
import br.com.conectabem.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtUtil jwtUtil;

    private UserController controller;

    @BeforeEach
    void setup() {
        controller = new UserController(userService, jwtUtil);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/users/register");
        request.setServerName("localhost");
        request.setServerPort(8080);
        request.setScheme("http");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void registerReturnsCreatedResponse() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@example.com");
        request.setPassword("senha123");
        request.setFullName("Novo Usuario");

        User created = User.builder()
                .id(UUID.randomUUID())
                .email("new@example.com")
                .password("hash")
                .fullName("Novo Usuario")
                .role(UserRole.USER.name())
                .createdAt(Instant.parse("2026-03-09T12:00:00Z"))
                .build();

        when(userService.register(request)).thenReturn(created);

        ResponseEntity<RegisterResponse> response = controller.register(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getHeaders().getLocation());
        assertTrue(response.getHeaders().getLocation().toString().endsWith("/users/register/" + created.getId()));
        assertEquals(created.getEmail(), response.getBody().getEmail());
    }

    @Test
    void registerReturnsConflictWhenServiceRejectsEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("dup@example.com");
        request.setPassword("senha123");
        request.setFullName("Dup");

        when(userService.register(request)).thenThrow(new IllegalArgumentException("email already registered"));

        ResponseEntity<RegisterResponse> response = controller.register(request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void loginReturnsTokenAndExpiry() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("senha123");
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email(request.getEmail())
                .password("hash")
                .fullName("User")
                .role(UserRole.USER.name())
                .createdAt(Instant.now())
                .build();

        when(userService.authenticate(request.getEmail(), request.getPassword())).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(userId, request.getEmail())).thenReturn("jwt-token");
        when(jwtUtil.getExpirationMinutes()).thenReturn(90L);

        ResponseEntity<LoginResponse> response = controller.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("jwt-token", response.getBody().getJwtToken());
        assertNotNull(response.getBody().getCreatedAt());
        assertNotNull(response.getBody().getExpiresAt());
    }

    @Test
    void loginReturnsUnauthorizedWhenCredentialsAreInvalid() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("wrong");

        when(userService.authenticate(request.getEmail(), request.getPassword())).thenReturn(Optional.empty());

        ResponseEntity<LoginResponse> response = controller.login(request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void meReturnsUnauthorizedWithoutAuthentication() {
        ResponseEntity<?> response = controller.me(null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void meReturnsNotFoundWhenAuthenticatedUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null);
        when(userService.findById(userId)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.me(authentication);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void meReturnsUserWhenAuthenticatedUserExists() {
        UUID userId = UUID.randomUUID();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null);
        User user = User.builder()
                .id(userId)
                .email("me@example.com")
                .password("hash")
                .fullName("Me")
                .role(UserRole.USER.name())
                .createdAt(Instant.now())
                .build();
        when(userService.findById(userId)).thenReturn(Optional.of(user));

        ResponseEntity<?> response = controller.me(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(user, response.getBody());
    }
}
