package br.com.conectabem.service;

import br.com.conectabem.dto.event.EventRegistrationDTO;
import br.com.conectabem.dto.event.JustifyAbsenceRequest;
import br.com.conectabem.dto.event.UpdateParticipationStatusRequest;

import java.util.List;
import java.util.UUID;

public interface EventRegistrationService {

    EventRegistrationDTO register(UUID eventId, UUID volunteerId);

    boolean cancelRegistration(UUID eventId, UUID volunteerId);

    List<EventRegistrationDTO> listParticipants(UUID eventId, UUID ownerId);

    List<EventRegistrationDTO> listVolunteerHistory(UUID volunteerId, boolean futureOnly);

    EventRegistrationDTO updateParticipationStatus(UUID eventId,
                                                   UUID volunteerId,
                                                   UpdateParticipationStatusRequest request,
                                                   UUID ownerId);

    EventRegistrationDTO justifyAbsence(UUID eventId,
                                        UUID volunteerId,
                                        JustifyAbsenceRequest request);
}
