package br.com.conectabem.repository;

import br.com.conectabem.model.User;
import br.com.conectabem.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
    }

    @Test
    void saveAndFindByEmail() {
        User u = new User();
        u.setUsername("repo");
        u.setEmail("repo@example.com");
        u.setPassword("HASH");
        u.setFullName("Repo User");
        u.setRole(UserRole.USER);

        User saved = userRepository.save(u);
        assertThat(saved.getId()).isNotNull();

        Optional<User> found = userRepository.findByEmail("repo@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("repo");
        assertThat(found.get().getFullName()).isEqualTo("Repo User");
        assertThat(found.get().getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    void findByEmailNotFound() {
        Optional<User> found = userRepository.findByEmail("notfound@example.com");
        assertThat(found).isEmpty();
    }

    @Test
    void findById() {
        User u = new User();
        u.setUsername("findbyid");
        u.setEmail("findbyid@example.com");
        u.setPassword("HASH");
        u.setFullName("Find By ID");
        u.setRole(UserRole.USER);

        UUID id = userRepository.save(u).getId();

        Optional<User> found = userRepository.findById(id);
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(id);
    }

    @Test
    void updateUser() {
        User u = new User();
        u.setUsername("update");
        u.setEmail("update@example.com");
        u.setPassword("OLD_HASH");
        u.setFullName("Old Name");
        u.setRole(UserRole.USER);

        User saved = userRepository.save(u);

        saved.setPassword("NEW_HASH");
        saved.setFullName("New Name");
        userRepository.save(saved);

        Optional<User> found = userRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getPassword()).isEqualTo("NEW_HASH");
        assertThat(found.get().getFullName()).isEqualTo("New Name");
    }

    @Test
    void deleteUser() {
        User u = new User();
        u.setUsername("delete");
        u.setEmail("delete@example.com");
        u.setPassword("HASH");
        u.setFullName("Delete User");
        u.setRole(UserRole.USER);

        UUID id = userRepository.save(u).getId();
        assertThat(userRepository.findById(id)).isPresent();

        userRepository.deleteById(id);
        assertThat(userRepository.findById(id)).isEmpty();
    }

    @Test
    void uniqueUsernameConstraint() {
        User u1 = new User();
        u1.setUsername("unique");
        u1.setEmail("u1@example.com");
        u1.setPassword("HASH1");
        u1.setFullName("User 1");
        u1.setRole(UserRole.USER);
        userRepository.save(u1);

        User u2 = new User();
        u2.setUsername("unique");
        u2.setEmail("u2@example.com");
        u2.setPassword("HASH2");
        u2.setFullName("User 2");
        u2.setRole(UserRole.USER);

        assertThatThrownBy(() -> userRepository.save(u2))
                .isInstanceOf(Exception.class);
    }
}

