package br.com.conectabem.infra;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigTest {

    @Test
    void passwordEncoderUsesBCrypt() {
        SecurityConfig config = new SecurityConfig(new JwtAuthFilter(null));

        PasswordEncoder encoder = config.passwordEncoder();
        String encoded = encoder.encode("senha123");

        assertNotEquals("senha123", encoded);
        assertTrue(encoder.matches("senha123", encoded));
    }
}

