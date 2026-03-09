package br.com.conectabem.dto.event;

import br.com.conectabem.model.ParticipationStatus;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class EventRegistrationDTO {
    private UUID id;
    private UUID eventId;
    private UUID volunteerId;
    private String volunteerName;
    private String volunteerEmail;
    private ParticipationStatus status;
    private String justification;
    private Instant registeredAt;
    private Instant statusUpdatedAt;
}
