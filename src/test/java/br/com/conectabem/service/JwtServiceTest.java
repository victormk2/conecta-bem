package br.com.conectabem.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JwtServiceTest {

    @Test
    void generateAndExtractUsername() {
        JwtService jwtService = new JwtService();
        String token = jwtService.generateToken("user");
        assertNotNull(token);
        assertEquals("user", jwtService.extractUsername(token));
    }
}

