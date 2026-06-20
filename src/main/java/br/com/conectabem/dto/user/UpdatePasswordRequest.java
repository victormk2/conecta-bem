package br.com.conectabem.dto.user;

public record UpdatePasswordRequest(
        String email,
        String temporaryPassword,
        String currentPassword,
        String newPassword
) {
}