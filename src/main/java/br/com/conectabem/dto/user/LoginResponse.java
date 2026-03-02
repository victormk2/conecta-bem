package br.com.conectabem.dto.user;

import lombok.Data;

import java.time.Instant;

@Data
public class LoginResponse {
    private String jwtToken;
    private Instant createdAt;
    private Instant expiresAt;
}
