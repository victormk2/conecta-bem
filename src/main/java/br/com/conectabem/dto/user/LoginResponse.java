package br.com.conectabem.dto.user;

import java.time.Instant;

public record LoginResponse (
    String jwtToken,
    Instant expiration
){}
