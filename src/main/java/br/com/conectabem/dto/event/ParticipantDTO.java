package br.com.conectabem.dto.event;

import br.com.conectabem.model.Gender;

import java.time.LocalDate;
import java.util.UUID;

public record ParticipantDTO(
        UUID registrationId,
        UUID id,
        String name,
        String email,
        String cpf,
        LocalDate birthDate,
        String phone,
        Gender gender,
        String status
) {}
