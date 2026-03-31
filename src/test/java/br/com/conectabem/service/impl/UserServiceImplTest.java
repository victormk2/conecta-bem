package br.com.conectabem.service.impl;

import br.com.conectabem.model.User;
import br.com.conectabem.model.UserRole;
import br.com.conectabem.repository.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Nested
    class FindByIdTest {
        @Test
        void shouldReturnUserWhenExists() {
            var userId = UUID.randomUUID();
            var user = new User();
            user.setId(userId);
            user.setUsername("john_doe");
            user.setEmail("john@example.com");
            user.setFullName("John Doe");
            user.setRole(UserRole.USER);
            user.setCreatedAt(Instant.now());

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            var result = userService.findById(userId);

            assertThat(result)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("id", userId)
                    .hasFieldOrPropertyWithValue("username", "john_doe")
                    .hasFieldOrPropertyWithValue("email", "john@example.com")
                    .hasFieldOrPropertyWithValue("fullName", "John Doe")
                    .hasFieldOrPropertyWithValue("role", UserRole.USER);
        }

        @Test
        void shouldReturnNullWhenUserDoesNotExist() {
            var userId = UUID.randomUUID();

            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            var result = userService.findById(userId);

            assertThat(result).isNull();
        }
    }
}

