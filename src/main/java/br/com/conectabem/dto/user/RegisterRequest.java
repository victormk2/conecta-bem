package br.com.conectabem.dto.user;

import br.com.conectabem.model.Gender;

import java.time.LocalDate;

public record RegisterRequest(
        String username,
        String password,
        String email,
        String fullName,
        String cpfCnpj,
        LocalDate birthDate,
        Gender gender,
        String phone,
        String instagram,
        String linkedin
) {
}