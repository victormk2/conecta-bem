package br.com.conectabem.dto.user;

import br.com.conectabem.model.User;

public record UserProfileResponse(
        String fullName,
        String cpfCnpj,
        String birthDate,
        String email,
        String gender,
        String phone,
        String instagram,
        String linkedin
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getFullName(),
                user.getCpfCnpj(),
                user.getBirthDate() != null ? user.getBirthDate().toString() : null,
                user.getEmail(),
                user.getGender() != null ? user.getGender().toString() : null,
                user.getPhone(),
                user.getInstagram(),
                user.getLinkedin()
        );
    }
}