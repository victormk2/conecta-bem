package br.com.conectabem.service;

import br.com.conectabem.dto.event.EnrollmentStatusDTO;
import br.com.conectabem.dto.event.ParticipantDTO;
import br.com.conectabem.dto.event.EventResponse;

import java.util.List;
import java.util.UUID;

public interface EventRegistrationService {
    void enroll(UUID eventId);
    void cancel(UUID eventId);
    EnrollmentStatusDTO getEnrollmentStatus(UUID eventId);
    List<ParticipantDTO> getParticipants(UUID eventId);
    List<EventResponse> getMyEnrolledEvents();
}