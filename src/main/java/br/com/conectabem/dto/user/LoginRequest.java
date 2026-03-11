package br.com.conectabem.dto.user;

public record LoginRequest(
        String username,
        String password
) {
}