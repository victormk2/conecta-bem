package br.com.conectabem.controller.user;

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
            var user = buildUser(userId);

            when(currentUserService.requireUserId()).thenReturn(userId);
            when(userService.findById(userId)).thenReturn(user);

            var response = userController.getProfile();

            verify(currentUserService).requireUserId();
            verify(userService).findById(userId);

            assertThat(response).isNotNull();
            assertThat(response.fullName()).isEqualTo("Joao Abc");
            assertThat(response.email()).isEqualTo("joao@gmail.com");
            assertThat(response.cpfOrCnpj()).isEqualTo("12345678910");
            assertThat(response.birthDate()).isEqualTo("2000-01-01");
            assertThat(response.gender()).isEqualTo("MALE");
            assertThat(response.phone()).isEqualTo("47999990000");
            assertThat(response.instagram()).isEqualTo("joao12");
            assertThat(response.linkedin()).isEqualTo("https://linkedin.com/in/joao321");
        }

        @Test
        void shouldDelegateToCorrectServices() {
            var userId = UUID.randomUUID();

            when(currentUserService.requireUserId()).thenReturn(userId);
            when(userService.findById(userId)).thenReturn(buildUser(userId));

            userController.getProfile();

            verify(currentUserService).requireUserId();
            verify(userService).findById(userId);
        }

        private User buildUser(UUID id) {
            var user = new User();
            user.setId(id);
            user.setFullName("Joao Abc");
            user.setEmail("joao@gmail.com");
            user.setUsername("joao123");
            user.setPassword("Abc123");
            user.setRole(UserRole.USER);
            user.setCpfCnpj("12345678910");
            user.setBirthDate(LocalDate.of(2000, 1, 1));
            user.setGender(Gender.MALE);
            user.setPhone("47999990000");
            user.setInstagram("joao12");
            user.setLinkedin("https://linkedin.com/in/joao321");
            return user;
        }
    }
}