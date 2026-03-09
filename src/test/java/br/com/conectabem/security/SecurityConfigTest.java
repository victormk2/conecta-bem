package br.com.conectabem.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigTest {

    @Test
    void passwordEncoderUsesBCrypt() {
        SecurityConfig config = new SecurityConfig(new JwtUtil("01234567890123456789012345678901", 15));

        PasswordEncoder encoder = config.passwordEncoder();
        String encoded = encoder.encode("senha123");

        assertNotEquals("senha123", encoded);
        assertTrue(encoder.matches("senha123", encoded));
    }
}
