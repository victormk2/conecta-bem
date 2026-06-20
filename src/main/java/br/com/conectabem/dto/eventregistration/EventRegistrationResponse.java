package br.com.conectabem.dto.eventregistration;

import br.com.conectabem.model.EventRegistration;

import java.time.Instant;
import java.util.UUID;

public record EventRegistrationResponse(
        UUID id,
        UUID eventId,
        UUID volunteerId,
        String status,
        String justification,
        Instant registeredAt,
        Instant statusUpdatedAt
) {
    public static EventRegistrationResponse from(EventRegistration registration) {
        return new EventRegistrationResponse(
                registration.getId(),
                registration.getEvent().getId(),
                registration.getVolunteer().getId(),
                registration.getStatus().name(),
                registration.getJustification(),
                registration.getRegisteredAt(),
                registration.getStatusUpdatedAt()
        );
    }
}
