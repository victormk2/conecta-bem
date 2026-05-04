package br.com.conectabem.controller.user;

import br.com.conectabem.controller.user.UserController;
import br.com.conectabem.dto.user.UpdateProfileRequest;
import br.com.conectabem.model.Gender;
import br.com.conectabem.model.User;
import br.com.conectabem.model.UserRole;
import br.com.conectabem.service.CurrentUserService;
import br.com.conectabem.service.UserService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @InjectMocks
    private UserController userController;

    @Mock
    private UserService userService;

    @Mock
    private CurrentUserService currentUserService;

    @Nested
    class GetProfileTest {

        @Test
        void shouldReturnProfileOfAuthenticatedUser() {
            var userId = UUID.randomUUID();
            when(currentUserService.requireUserId()).thenReturn(userId);
            when(userService.findById(userId)).thenReturn(buildUser(userId));

            var response = userController.getProfile();

            verify(currentUserService).requireUserId();
            verify(userService).findById(userId);

            assertThat(response.fullName()).isEqualTo("João Silva");
            assertThat(response.email()).isEqualTo("joao@gmail.com");
            assertThat(response.cpfCnpj()).isEqualTo("12345678901");
            assertThat(response.birthDate()).isEqualTo("2000-01-15");
            assertThat(response.phone()).isEqualTo("47999990000");
            assertThat(response.instagram()).isEqualTo("joao123");
            assertThat(response.linkedin()).isEqualTo("https://linkedin.com/in/joao123");
        }
    }

    @Nested
    class UpdateProfileTest {

        @Test
        void shouldCallServiceAndReturnNoContent() {
            var userId = UUID.randomUUID();
            var request = new UpdateProfileRequest(
                    "joao@gmail.com", null, Gender.MALE, "47911110000", "joao123", "https://linkedin.com/in/joao123"
            );

            when(currentUserService.requireUserId()).thenReturn(userId);

            var response = userController.updateProfile(request);

            verify(currentUserService).requireUserId();
            verify(userService).updateProfile(userId, request);
            assertThat(response.getStatusCode().value()).isEqualTo(NO_CONTENT.value());
        }

        @Test
        void shouldPassCorrectUserIdToService() {
            var userId = UUID.randomUUID();
            var request = new UpdateProfileRequest(null, null, null, null, "joao123", null);

            when(currentUserService.requireUserId()).thenReturn(userId);

            userController.updateProfile(request);

            verify(userService).updateProfile(userId, request);
        }
    }

    private User buildUser(UUID id) {
        var user = new User();
        user.setId(id);
        user.setFullName("João Silva");
        user.setEmail("joao@gmail.com");
        user.setUsername("joao123");
        user.setPassword("HASH");
        user.setRole(UserRole.USER);
        user.setCpfCnpj("12345678901");
        user.setBirthDate(LocalDate.of(2000, 1, 15));
        user.setGender(Gender.MALE);
        user.setPhone("47999990000");
        user.setInstagram("joao123");
        user.setLinkedin("https://linkedin.com/in/joao123");
        return user;
    }
}