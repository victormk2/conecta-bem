package br.com.conectabem.dto.user;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class RegisterResponse {
    private UUID id;
    private String email;
    private Instant createdAt;
}

