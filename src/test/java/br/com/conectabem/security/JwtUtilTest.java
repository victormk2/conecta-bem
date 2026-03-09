package br.com.conectabem.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    @Test
    void generateAndValidate() {
        String secret = "01234567890123456789012345678901"; // 32 chars
        long expirationMinutes = 1L;
        JwtUtil jwtUtil = new JwtUtil(secret, expirationMinutes);

        UUID id = UUID.randomUUID();
        String token = jwtUtil.generateToken(id, "a@b.com");
        assertNotNull(token);

        Jws<Claims> parsed = jwtUtil.validateToken(token);
        assertEquals(id.toString(), parsed.getBody().getSubject());
        assertEquals("a@b.com", parsed.getBody().get("email"));
        assertEquals(expirationMinutes, jwtUtil.getExpirationMinutes());
    }
}
