package br.com.conectabem.service;

import br.com.conectabem.dto.user.RegisterRequest;
import br.com.conectabem.model.User;
import br.com.conectabem.model.UserRole;
import br.com.conectabem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserServiceImplTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
    }

    @Test
    void registerAndFindUser() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@example.com");
        req.setPassword("senha123");
        req.setFullName("Test User");

        User created = userService.register(req);

        assertNotNull(created.getId());
        assertEquals("test@example.com", created.getEmail());
        assertNotNull(created.getPassword());
        assertEquals(UserRole.USER.name(), created.getRole());
        assertTrue(passwordEncoder.matches("senha123", created.getPassword()));

        Optional<User> found = userRepository.findByEmail("test@example.com");
        assertTrue(found.isPresent());
        assertEquals(created.getId(), found.get().getId());
    }

    @Test
    void authenticateWithCorrectPassword() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("auth@example.com");
        req.setPassword("senha123");
        req.setFullName("Auth User");

        User registered = userService.register(req);

        Optional<User> authenticated = userService.authenticate("auth@example.com", "senha123");
        assertTrue(authenticated.isPresent());
        assertEquals(registered.getId(), authenticated.get().getId());
    }

    @Test
    void authenticateWithWrongPassword() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("wrong@example.com");
        req.setPassword("senha123");
        req.setFullName("Wrong User");

        userService.register(req);

        Optional<User> authenticated = userService.authenticate("wrong@example.com", "wrongpassword");
        assertFalse(authenticated.isPresent());
    }

    @Test
    void registerDuplicateEmailThrowsException() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("dup@example.com");
        req.setPassword("senha123");
        req.setFullName("Dup User");

        userService.register(req);

        assertThrows(IllegalArgumentException.class, () -> userService.register(req));
    }
}
