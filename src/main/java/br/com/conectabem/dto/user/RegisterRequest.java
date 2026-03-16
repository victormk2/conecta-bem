package br.com.conectabem.dto.user;

public record RegisterRequest(
        String username,
        String password,
        String email,
        String fullName
) {
}