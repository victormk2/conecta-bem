package br.com.conectabem.service;

import br.com.conectabem.dto.user.LoginRequest;
import br.com.conectabem.dto.user.RegisterRequest;
import br.com.conectabem.model.Gender;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
        RegisterRequest request = createRegisterRequest();

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
        assertEquals("12345678901",                      saved.getCpfCnpj());
        assertEquals(LocalDate.of(2000, 1, 15),          saved.getBirthDate());
        assertEquals(Gender.MALE,                        saved.getGender());
        assertEquals("47999990000",                      saved.getPhone());
        assertEquals("userinstagram",                    saved.getInstagram());
        assertEquals("https://linkedin.com/in/u",        saved.getLinkedin());
    }

    @Test
    void registerPersistsNullForOptionalFields() {
        var request = new RegisterRequest(
                "u", "senha123", "u@e.com", "User",
                "12345678901", LocalDate.of(1995, 6, 15),
                Gender.PREFER_NOT_TO_SAY, "47999990000", null, null
        );

        when(passwordEncoder.encode(any())).thenReturn("HASH");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generateToken(any())).thenReturn("jwt");

        authService.register(request);

        var captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        var saved = captor.getValue();

        assertNull(saved.getInstagram());
        assertNull(saved.getLinkedin());
    }

    @Test
    void loginReturnsJwtWhenPasswordMatches() {
        User user = new User();
        user.setUsername("u");
        user.setEmail("u@e.com");
        user.setFullName("User");
        user.setRole(UserRole.USER);
        user.setPassword("HASH");

        when(userRepository.findByEmail("u@e.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senha123", "HASH")).thenReturn(true);
        when(jwtService.generateToken("u")).thenReturn("jwt");

        String token = authService.login(new LoginRequest("u@e.com", "senha123"));

        assertEquals("jwt", token);
    }

    @Test
    void loginThrowsWhenPasswordDoesNotMatch() {
        User user = new User();
        user.setUsername("u");
        user.setEmail("u@e.com");
        user.setPassword("HASH");

        when(userRepository.findByEmail("u@e.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "HASH")).thenReturn(false);

        assertThrows(RuntimeException.class, () -> authService.login(new LoginRequest("u@e.com", "wrong")));
    }

    @Test
    void loginThrowsWhenTemporaryPasswordIsUsed() {
        User user = new User();
        user.setUsername("u");
        user.setEmail("u@e.com");
        user.setPassword("MAIN_HASH");
        user.setTemporaryPassword("TEMP_HASH");
        user.setTemporaryPasswordExpiresAt(Instant.now().plusSeconds(300));

        when(userRepository.findByEmail("u@e.com")).thenReturn(Optional.of(user));
        // senha temporária não bate com o campo password (principal)
        when(passwordEncoder.matches("temp-raw", "MAIN_HASH")).thenReturn(false);

        assertThrows(RuntimeException.class,
                () -> authService.login(new LoginRequest("u@e.com", "temp-raw")));
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

    RegisterRequest createRegisterRequest(){
        return new RegisterRequest(
                "u", "senha123", "u@e.com", "User",
                "12345678901", LocalDate.of(2000, 1, 15),
                Gender.MALE, "47999990000", "userinstagram", "https://linkedin.com/in/u"
        );
    }
}
