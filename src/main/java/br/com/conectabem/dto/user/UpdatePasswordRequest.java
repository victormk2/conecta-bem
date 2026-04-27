package br.com.conectabem.dto.user;

public record UpdatePasswordRequest(
        String email,
        String currentPassword,
        String newPassword
) {
}