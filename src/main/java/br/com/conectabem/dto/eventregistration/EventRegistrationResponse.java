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
        String organizerFeedback,
        Integer feedbackRating,
        Instant feedbackCreatedAt,
        Instant registeredAt,
        Instant statusUpdatedAt
) {
    public EventRegistrationResponse(
            UUID id,
            UUID eventId,
            UUID volunteerId,
            String status,
            String justification,
            Instant registeredAt,
            Instant statusUpdatedAt
    ) {
        this(id, eventId, volunteerId, status, justification, null, null, null, registeredAt, statusUpdatedAt);
    }

    public static EventRegistrationResponse from(EventRegistration registration) {
        return new EventRegistrationResponse(
                registration.getId(),
                registration.getEvent().getId(),
                registration.getVolunteer().getId(),
                registration.getStatus().name(),
                registration.getJustification(),
                registration.getOrganizerFeedback(),
                registration.getFeedbackRating(),
                registration.getFeedbackCreatedAt(),
                registration.getRegisteredAt(),
                registration.getStatusUpdatedAt()
        );
    }
}
