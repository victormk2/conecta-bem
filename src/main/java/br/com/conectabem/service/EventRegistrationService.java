package br.com.conectabem.service;

import br.com.conectabem.dto.event.EventRegistrationDTO;
import br.com.conectabem.dto.event.JustifyAbsenceRequest;
import br.com.conectabem.dto.event.UpdateParticipationStatusRequest;

import java.util.List;
import java.util.UUID;

public interface EventRegistrationService {

    EventRegistrationDTO register(UUID eventId);

    boolean cancelRegistration(UUID eventId);

    List<EventRegistrationDTO> listParticipants(UUID eventId);

    List<EventRegistrationDTO> myRegistrations(boolean futureOnly);

    EventRegistrationDTO updateParticipationStatus(UUID eventId,
                                                   UUID volunteerId,
                                                   UpdateParticipationStatusRequest request);

    EventRegistrationDTO justifyAbsence(UUID eventId,
                                        JustifyAbsenceRequest request);
}
