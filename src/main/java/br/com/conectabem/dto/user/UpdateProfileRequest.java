package br.com.conectabem.dto.user;

import br.com.conectabem.model.Gender;

public record UpdateProfileRequest(
        String email,
        String cpfCnpj,
        Gender gender,
        String phone,
        String instagram,
        String linkedin
) {
}