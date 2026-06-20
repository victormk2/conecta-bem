package br.com.conectabem.service;

import br.com.conectabem.model.User;
import br.com.conectabem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JavaMailSender mailSender;
    @Mock private MimeMessage mimeMessage;

    private PasswordResetService passwordResetService;

    @BeforeEach
    void setup() {
        passwordResetService = new PasswordResetService(userRepository, passwordEncoder, mailSender);
    }

    @Nested
    class ForgotPasswordTest {

        @Test
        void shouldSaveTemporaryPasswordWithoutChangingMainPassword() {
            var user = buildUser();
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.encode(anyString())).thenReturn("ENCODED_TEMP");
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            passwordResetService.forgotPassword("user@test.com");

            var captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());

            var saved = captor.getValue();
            assertThat(saved.getPassword()).isEqualTo("OLD_HASH");
            assertThat(saved.getTemporaryPassword()).isEqualTo("ENCODED_TEMP");
        }

        @Test
        void shouldSetTemporaryPasswordExpirationToFiveMinutes() {
            var user = buildUser();
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.encode(anyString())).thenReturn("ENCODED_TEMP");
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            var before = Instant.now().plusSeconds(299);
            passwordResetService.forgotPassword("user@test.com");
            var after = Instant.now().plusSeconds(301);

            var captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());

            var expiry = captor.getValue().getTemporaryPasswordExpiresAt();
            assertThat(expiry).isAfter(before).isBefore(after);
        }

        @Test
        void shouldSendEmailWhenUserExists() {
            var user = buildUser();
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.encode(anyString())).thenReturn("ENCODED_TEMP");
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            passwordResetService.forgotPassword("user@test.com");

            verify(mailSender).send(mimeMessage);
        }

        @Test
        void shouldDoNothingWhenEmailDoesNotExist() {
            when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

            passwordResetService.forgotPassword("unknown@test.com");

            verify(userRepository, never()).save(any());
            verify(mailSender, never()).send(any(MimeMessage.class));
        }

        @Test
        void shouldGenerateDifferentTemporaryPasswordsOnEachCall() {
            var user1 = buildUser();
            var user2 = buildUser();

            when(userRepository.findByEmail("a@test.com")).thenReturn(Optional.of(user1));
            when(userRepository.findByEmail("b@test.com")).thenReturn(Optional.of(user2));
            when(passwordEncoder.encode(anyString())).thenAnswer(i -> "ENCODED:" + i.getArgument(0));
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            passwordResetService.forgotPassword("a@test.com");
            passwordResetService.forgotPassword("b@test.com");

            var captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository, times(2)).save(captor.capture());

            var passwords = captor.getAllValues().stream()
                    .map(User::getTemporaryPassword)
                    .toList();

            assertThat(passwords.get(0)).isNotEqualTo(passwords.get(1));
        }

        @Test
        void shouldGenerateTemporaryPasswordWithExpectedLength() {
            var user = buildUser();
            var rawPasswordCaptor = ArgumentCaptor.forClass(String.class);

            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.encode(rawPasswordCaptor.capture())).thenReturn("ENCODED_TEMP");
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            passwordResetService.forgotPassword("user@test.com");

            assertThat(rawPasswordCaptor.getValue()).hasSize(10);
        }
    }

    private User buildUser() {
        var user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        user.setEmail("user@test.com");
        user.setPassword("OLD_HASH");
        return user;
    }
}