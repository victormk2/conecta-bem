package br.com.conectabem.service.impl;

import br.com.conectabem.dto.user.UpdatePasswordRequest;
import br.com.conectabem.dto.user.UpdateProfileRequest;
import br.com.conectabem.model.Gender;
import br.com.conectabem.model.User;
import br.com.conectabem.model.UserRole;
import br.com.conectabem.repository.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Nested
    class FindByIdTest {

        @Test
        void shouldReturnUserWhenExists() {
            var userId = UUID.randomUUID();
            var user = buildUser(userId);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            var result = userService.findById(userId);

            assertThat(result)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("id", userId)
                    .hasFieldOrPropertyWithValue("username", "joao123")
                    .hasFieldOrPropertyWithValue("email", "joao@gmail.com")
                    .hasFieldOrPropertyWithValue("fullName", "João Silva")
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

    @Nested
    class UpdateProfileTest {

        @Test
        void shouldUpdateAllEditableFields() {
            var userId = UUID.randomUUID();
            var user = buildUser(userId);
            var request = new UpdateProfileRequest(
                    "joao@gmail.com", "47304057000148", Gender.FEMALE, "47911110000", "joao123", "https://linkedin.com/in/joao123"
            );

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            userService.updateProfile(userId, request);

            var captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());

            var saved = captor.getValue();
            assertThat(saved.getEmail()).isEqualTo("joao@gmail.com");
            assertThat(saved.getCpfCnpj()).isEqualTo("47304057000148");
            assertThat(saved.getGender()).isEqualTo(Gender.FEMALE);
            assertThat(saved.getPhone()).isEqualTo("47911110000");
            assertThat(saved.getInstagram()).isEqualTo("joao123");
            assertThat(saved.getLinkedin()).isEqualTo("https://linkedin.com/in/joao123");
        }

        @Test
        void shouldNotOverwriteFieldsWhenNull() {
            var userId = UUID.randomUUID();
            var user = buildUser(userId);
            user.setEmail("joao@gmail.com");
            user.setPhone("47999990000");

            var request = new UpdateProfileRequest(null, null, null, null, "joao123", null);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            userService.updateProfile(userId, request);

            var captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());

            var saved = captor.getValue();
            assertThat(saved.getEmail()).isEqualTo("joao@gmail.com");
            assertThat(saved.getPhone()).isEqualTo("47999990000");
            assertThat(saved.getInstagram()).isEqualTo("joao123");
        }

        @Test
        void shouldThrowNotFoundWhenUserDoesNotExist() {
            var userId = UUID.randomUUID();
            var request = new UpdateProfileRequest("joao@gmail.com", null, null, null, null, null);

            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateProfile(userId, request))
                    .isInstanceOf(ResponseStatusException.class);

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    class UpdatePasswordTest {

        @Test
        void shouldUpdatePasswordSuccessfullyWithCurrentPassword() {
            var userId = UUID.randomUUID();
            var user = buildUser(userId);
            user.setPassword("encoded-old");
            user.setEmail("user@test.com");

            var request = new UpdatePasswordRequest("user@test.com", null, "old-pass", "new-pass");

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("old-pass", "encoded-old")).thenReturn(true);
            when(passwordEncoder.matches("new-pass", "encoded-old")).thenReturn(false);
            when(passwordEncoder.encode("new-pass")).thenReturn("encoded-new");

            userService.updatePassword(userId, request);

            var captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());

            var saved = captor.getValue();
            assertThat(saved.getPassword()).isEqualTo("encoded-new");
            assertThat(saved.getTemporaryPassword()).isNull();
            assertThat(saved.getTemporaryPasswordExpiresAt()).isNull();
        }

        @Test
        void shouldThrowWhenCurrentPasswordIsWrong() {
            var userId = UUID.randomUUID();
            var user = buildUser(userId);
            user.setPassword("encoded-old");
            user.setEmail("user@test.com");

            var request = new UpdatePasswordRequest("user@test.com", null, "wrong-pass", "new-pass");

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong-pass", "encoded-old")).thenReturn(false);

            assertThatThrownBy(() -> userService.updatePassword(userId, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Current password is incorrect");

            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldThrowWhenCurrentPasswordIsBlank() {
            var userId = UUID.randomUUID();
            var user = buildUser(userId);
            user.setEmail("user@test.com");

            var request = new UpdatePasswordRequest("user@test.com", null, "", "new-pass");

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.updatePassword(userId, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Current password is required");

            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldUpdatePasswordSuccessfullyWithTemporaryPassword() {
            var userId = UUID.randomUUID();
            var user = buildUser(userId);
            user.setPassword("encoded-old");
            user.setEmail("user@test.com");
            user.setTemporaryPassword("encoded-temp");
            user.setTemporaryPasswordExpiresAt(Instant.now().plusSeconds(300));

            var request = new UpdatePasswordRequest("user@test.com", "temp-pass", null, "new-pass");

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("temp-pass", "encoded-temp")).thenReturn(true);
            when(passwordEncoder.matches("new-pass", "encoded-old")).thenReturn(false);
            when(passwordEncoder.encode("new-pass")).thenReturn("encoded-new");

            userService.updatePassword(userId, request);

            var captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());

            var saved = captor.getValue();
            assertThat(saved.getPassword()).isEqualTo("encoded-new");
            assertThat(saved.getTemporaryPassword()).isNull();
            assertThat(saved.getTemporaryPasswordExpiresAt()).isNull();
        }

        @Test
        void shouldThrowWhenTemporaryPasswordIsExpired() {
            var userId = UUID.randomUUID();
            var user = buildUser(userId);
            user.setEmail("user@test.com");
            user.setTemporaryPassword("encoded-temp");
            user.setTemporaryPasswordExpiresAt(Instant.now().minusSeconds(1));

            var request = new UpdatePasswordRequest("user@test.com", "temp-pass", null, "new-pass");

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.updatePassword(userId, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.GONE));

            var captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getTemporaryPassword()).isNull();
            assertThat(captor.getValue().getTemporaryPasswordExpiresAt()).isNull();
        }

        @Test
        void shouldThrowWhenTemporaryPasswordIsInvalid() {
            var userId = UUID.randomUUID();
            var user = buildUser(userId);
            user.setEmail("user@test.com");
            user.setTemporaryPassword("encoded-temp");
            user.setTemporaryPasswordExpiresAt(Instant.now().plusSeconds(300));

            var request = new UpdatePasswordRequest("user@test.com", "wrong-temp", null, "new-pass");

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong-temp", "encoded-temp")).thenReturn(false);

            assertThatThrownBy(() -> userService.updatePassword(userId, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Invalid temporary password");

            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldThrowWhenTemporaryPasswordFieldIsNullInUser() {
            var userId = UUID.randomUUID();
            var user = buildUser(userId);
            user.setEmail("user@test.com");
            user.setTemporaryPassword(null);

            var request = new UpdatePasswordRequest("user@test.com", "temp-pass", null, "new-pass");

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.updatePassword(userId, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("No temporary password set");

            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldThrowWhenEmailDoesNotMatch() {
            var userId = UUID.randomUUID();
            var user = buildUser(userId);
            user.setEmail("correct@test.com");

            var request = new UpdatePasswordRequest("wrong@test.com", null, "old-pass", "new-pass");

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.updatePassword(userId, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Email mismatch");

            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldThrowWhenNewPasswordIsSameAsCurrentPassword() {
            var userId = UUID.randomUUID();
            var user = buildUser(userId);
            user.setPassword("encoded-old");
            user.setEmail("user@test.com");

            var request = new UpdatePasswordRequest("user@test.com", null, "old-pass", "old-pass");

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("old-pass", "encoded-old")).thenReturn(true);

            assertThatThrownBy(() -> userService.updatePassword(userId, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("New password must be different");

            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldThrowWhenNewPasswordIsBlank() {
            var userId = UUID.randomUUID();
            var user = buildUser(userId);
            user.setEmail("user@test.com");

            var request = new UpdatePasswordRequest("user@test.com", null, "old-pass", "");

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.updatePassword(userId, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("New password is required");

            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldThrowWhenUserDoesNotExist() {
            var userId = UUID.randomUUID();
            var request = new UpdatePasswordRequest("user@test.com", null, "old-pass", "new-pass");

            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updatePassword(userId, request))
                    .isInstanceOf(ResponseStatusException.class);

            verify(userRepository, never()).save(any());
        }
    }

    private User buildUser(UUID id) {
        var user = new User();
        user.setId(id);
        user.setUsername("joao123");
        user.setEmail("joao@gmail.com");
        user.setFullName("João Silva");
        user.setPassword("HASH");
        user.setRole(UserRole.USER);
        user.setCreatedAt(Instant.now());
        return user;
    }
}