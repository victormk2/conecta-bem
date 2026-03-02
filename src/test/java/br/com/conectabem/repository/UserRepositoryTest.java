package br.com.conectabem.repository;

import br.com.conectabem.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
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
        User u = User.builder()
                .id(UUID.randomUUID())
                .email("repo@example.com")
                .password("HASH")
                .fullName("Repo User")
                .createdAt(Instant.now())
                .build();

        userRepository.save(u);

        Optional<User> found = userRepository.findByEmail("repo@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("repo@example.com");
        assertThat(found.get().getFullName()).isEqualTo("Repo User");
    }

    @Test
    void findByEmailNotFound() {
        Optional<User> found = userRepository.findByEmail("notfound@example.com");
        assertThat(found).isEmpty();
    }

    @Test
    void findById() {
        UUID id = UUID.randomUUID();
        User u = User.builder()
                .id(id)
                .email("findbyid@example.com")
                .password("HASH")
                .fullName("Find By ID")
                .createdAt(Instant.now())
                .build();

        userRepository.save(u);

        Optional<User> found = userRepository.findById(id);
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(id);
    }

    @Test
    void updateUser() {
        UUID id = UUID.randomUUID();
        User u = User.builder()
                .id(id)
                .email("update@example.com")
                .password("OLD_HASH")
                .fullName("Old Name")
                .createdAt(Instant.now())
                .build();

        userRepository.save(u);

        u.setPassword("NEW_HASH");
        u.setFullName("New Name");
        userRepository.save(u);

        Optional<User> found = userRepository.findById(id);
        assertThat(found).isPresent();
        assertThat(found.get().getPassword()).isEqualTo("NEW_HASH");
        assertThat(found.get().getFullName()).isEqualTo("New Name");
    }

    @Test
    void deleteUser() {
        UUID id = UUID.randomUUID();
        User u = User.builder()
                .id(id)
                .email("delete@example.com")
                .password("HASH")
                .fullName("Delete User")
                .createdAt(Instant.now())
                .build();

        userRepository.save(u);
        assertThat(userRepository.findById(id)).isPresent();

        userRepository.deleteById(id);
        assertThat(userRepository.findById(id)).isEmpty();
    }

    @Test
    void uniqueEmailConstraint() {
        UUID id1 = UUID.randomUUID();
        User u1 = User.builder()
                .id(id1)
                .email("unique@example.com")
                .password("HASH1")
                .fullName("User 1")
                .createdAt(Instant.now())
                .build();

        userRepository.save(u1);

        UUID id2 = UUID.randomUUID();
        User u2 = User.builder()
                .id(id2)
                .email("unique@example.com")
                .password("HASH2")
                .fullName("User 2")
                .createdAt(Instant.now())
                .build();

        // Try to save user with duplicate email - should throw exception
        assertThatThrownBy(() -> userRepository.save(u2))
                .isInstanceOf(Exception.class);
    }
}
