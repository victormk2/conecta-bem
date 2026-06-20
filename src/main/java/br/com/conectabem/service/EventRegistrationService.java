package br.com.conectabem.service;

import br.com.conectabem.dto.eventregistration.EventRegistrationDecisionRequest;
import br.com.conectabem.dto.eventregistration.EventRegistrationResponse;

import java.util.List;

public interface EventRegistrationService {
    EventRegistrationResponse register(String eventId);

    EventRegistrationResponse confirm(String registrationId);

    EventRegistrationResponse reject(String registrationId, EventRegistrationDecisionRequest request);

    EventRegistrationResponse dismiss(String registrationId, EventRegistrationDecisionRequest request);

    List<EventRegistrationResponse> listByEvent(String eventId);

    List<EventRegistrationResponse> listCurrentUserRegistrations();
}
