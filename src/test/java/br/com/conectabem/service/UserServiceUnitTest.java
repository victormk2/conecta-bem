package br.com.conectabem.service;

import br.com.conectabem.dto.user.RegisterRequest;
import br.com.conectabem.model.User;
import br.com.conectabem.model.UserRole;
import br.com.conectabem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserServiceImpl userService;

    @BeforeEach
    void setup() {
        userService = new UserServiceImpl(userRepository, passwordEncoder);
    }

    @Test
    void registerHashesPasswordAndSaves() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("u@e.com");
        req.setPassword("plainpwd");
        req.setFullName("User");

        when(userRepository.findByEmail("u@e.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plainpwd")).thenReturn("HASH");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = userService.register(req);

        assertNotNull(created.getId());
        assertEquals("u@e.com", created.getEmail());
        assertEquals("HASH", created.getPassword());
        assertEquals("User", created.getFullName());
        assertEquals(UserRole.USER.name(), created.getRole());
        assertNotNull(created.getCreatedAt());

        verify(userRepository).save(any(User.class));
    }

    @Test
    void authenticateReturnsUserWhenPasswordMatches() {
        String raw = "rawpwd";
        User u = User.builder()
                .id(UUID.randomUUID())
                .email("a@b.com")
                .password("HASH")
                .role(UserRole.USER.name())
                .build();
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches(raw, "HASH")).thenReturn(true);

        Optional<User> auth = userService.authenticate("a@b.com", raw);
        assertTrue(auth.isPresent());
        assertEquals(u.getId(), auth.get().getId());
    }

    @Test
    void findByEmailAndFindByIdDelegateToRepository() {
        UUID id = UUID.randomUUID();
        User user = User.builder()
                .id(id)
                .email("find@example.com")
                .password("HASH")
                .role(UserRole.USER.name())
                .build();

        when(userRepository.findByEmail("find@example.com")).thenReturn(Optional.of(user));
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        assertTrue(userService.findByEmail("find@example.com").isPresent());
        assertTrue(userService.findById(id).isPresent());
    }
}
