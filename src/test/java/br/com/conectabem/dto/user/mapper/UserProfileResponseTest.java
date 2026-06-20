package br.com.conectabem.dto.user.mapper;

import br.com.conectabem.dto.user.UserProfileResponse;
import br.com.conectabem.model.Gender;
import br.com.conectabem.model.User;
import br.com.conectabem.model.UserRole;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserProfileResponseTest {

    @Nested
    class FromTest {

        @Test
        void shouldMapAllFieldsFromUser() {
            var user = buildFullUser();

            var response = UserProfileResponse.from(user);

            assertThat(response.fullName()).isEqualTo("Joao Silva");
            assertThat(response.email()).isEqualTo("joao@gmail.com");
            assertThat(response.cpfCnpj()).isEqualTo("98765432100");
            assertThat(response.birthDate()).isEqualTo("2000-01-01");
            assertThat(response.gender()).isEqualTo("OTHER");
            assertThat(response.phone()).isEqualTo("47988880000");
            assertThat(response.instagram()).isEqualTo("joao12");
            assertThat(response.linkedin()).isEqualTo("https://linkedin.com/in/joao321");
        }

        @Test
        void shouldReturnNullBirthDateWhenNotSet() {
            var user = buildFullUser();
            user.setBirthDate(null);

            var response = UserProfileResponse.from(user);

            assertThat(response.birthDate()).isNull();
        }

        @Test
        void shouldReturnNullOptionalFieldsWhenNotSet() {
            var user = buildFullUser();
            user.setCpfCnpj(null);
            user.setGender(null);
            user.setPhone(null);
            user.setInstagram(null);
            user.setLinkedin(null);

            var response = UserProfileResponse.from(user);

            assertThat(response.cpfCnpj()).isNull();
            assertThat(response.gender()).isNull();
            assertThat(response.phone()).isNull();
            assertThat(response.instagram()).isNull();
            assertThat(response.linkedin()).isNull();
        }

        @Test
        void shouldFormatBirthDateAsIso8601() {
            var user = buildFullUser();
            user.setBirthDate(LocalDate.of(2000, 12, 31));

            var response = UserProfileResponse.from(user);

            assertThat(response.birthDate()).isEqualTo("2000-12-31");
        }

        private User buildFullUser() {
            var user = new User();
            user.setId(UUID.randomUUID());
            user.setUsername("joao123");
            user.setEmail("joao@gmail.com");
            user.setFullName("Joao Silva");
            user.setPassword("Abc123");
            user.setRole(UserRole.USER);
            user.setCpfCnpj("98765432100");
            user.setBirthDate(LocalDate.of(2000, 1, 1));
            user.setGender(Gender.OTHER);
            user.setPhone("47988880000");
            user.setInstagram("joao12");
            user.setLinkedin("https://linkedin.com/in/joao321");
            return user;
        }
    }
}