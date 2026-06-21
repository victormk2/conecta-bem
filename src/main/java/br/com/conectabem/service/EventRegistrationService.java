package br.com.conectabem.service;

import br.com.conectabem.dto.event.EnrollmentStatusDTO;
import br.com.conectabem.dto.event.ParticipantDTO;
import br.com.conectabem.dto.event.EventResponse;
import br.com.conectabem.dto.eventregistration.AbsenceNoticeRequest;
import br.com.conectabem.dto.eventregistration.EventRegistrationDecisionRequest;
import br.com.conectabem.dto.eventregistration.EventRegistrationResponse;
import br.com.conectabem.dto.eventregistration.OrganizerFeedbackRequest;

import java.util.List;
import java.util.UUID;

public interface EventRegistrationService {
    void enroll(UUID eventId);
    void cancel(UUID eventId);
    EventRegistrationResponse notifyAbsence(UUID eventId, AbsenceNoticeRequest request);
    EnrollmentStatusDTO getEnrollmentStatus(UUID eventId);
    List<ParticipantDTO> getParticipants(UUID eventId);
    List<EventResponse> getMyEnrolledEvents();
    EventRegistrationResponse confirm(UUID registrationId);
    EventRegistrationResponse reject(UUID registrationId, EventRegistrationDecisionRequest request);
    EventRegistrationResponse dismiss(UUID registrationId, EventRegistrationDecisionRequest request);
    EventRegistrationResponse addOrganizerFeedback(UUID registrationId, OrganizerFeedbackRequest request);
    List<EventRegistrationResponse> listByEvent(UUID eventId);
}
