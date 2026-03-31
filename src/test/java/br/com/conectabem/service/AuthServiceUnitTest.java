package br.com.conectabem.service;

import br.com.conectabem.dto.user.LoginRequest;
import br.com.conectabem.dto.user.RegisterRequest;
import br.com.conectabem.model.User;
import br.com.conectabem.model.UserRole;
import br.com.conectabem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setup() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void registerEncodesPasswordPersistsUserAndReturnsJwt() {
        RegisterRequest request = new RegisterRequest("u", "senha123", "u@e.com", "User");

        when(passwordEncoder.encode("senha123")).thenReturn("HASH");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken("u")).thenReturn("jwt");

        String token = authService.register(request);

        assertEquals("jwt", token);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("u", saved.getUsername());
        assertEquals("u@e.com", saved.getEmail());
        assertEquals("User", saved.getFullName());
        assertEquals(UserRole.USER, saved.getRole());
        assertEquals("HASH", saved.getPassword());
    }

    @Test
    void loginReturnsJwtWhenPasswordMatches() {
        User user = new User();
        user.setUsername("u");
        user.setEmail("u@e.com");
        user.setFullName("User");
        user.setRole(UserRole.USER);
        user.setPassword("HASH");

        when(userRepository.findByUsername("u")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senha123", "HASH")).thenReturn(true);
        when(jwtService.generateToken("u")).thenReturn("jwt");

        String token = authService.login(new LoginRequest("u", "senha123"));

        assertEquals("jwt", token);
    }

    @Test
    void loginThrowsWhenPasswordDoesNotMatch() {
        User user = new User();
        user.setUsername("u");
        user.setPassword("HASH");

        when(userRepository.findByUsername("u")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "HASH")).thenReturn(false);

        assertThrows(RuntimeException.class, () -> authService.login(new LoginRequest("u", "wrong")));
    }

    @Test
    void checkAccessReturnsTrueWhenTokenIsValidAndUserExists() {
        when(jwtService.extractUsername("token")).thenReturn("u");
        when(userRepository.findByUsername("u")).thenReturn(Optional.of(new User()));

        assertTrue(authService.checkAccess("token"));
    }

    @Test
    void checkAccessReturnsFalseWhenTokenIsInvalid() {
        when(jwtService.extractUsername("token")).thenThrow(new RuntimeException("bad token"));

        assertFalse(authService.checkAccess("token"));
    }
}
