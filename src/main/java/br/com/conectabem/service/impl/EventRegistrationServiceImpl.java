package br.com.conectabem.service.impl;

import br.com.conectabem.dto.eventregistration.EventRegistrationDecisionRequest;
import br.com.conectabem.dto.eventregistration.EventRegistrationResponse;
import br.com.conectabem.model.Event;
import br.com.conectabem.model.EventRegistration;
import br.com.conectabem.model.ParticipationStatus;
import br.com.conectabem.model.User;
import br.com.conectabem.repository.EventRegistrationRepository;
import br.com.conectabem.repository.EventRepository;
import br.com.conectabem.service.CurrentUserService;
import br.com.conectabem.service.EventRegistrationService;
import br.com.conectabem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventRegistrationServiceImpl implements EventRegistrationService {

    private static final Set<ParticipationStatus> CAPACITY_STATUSES = Set.of(
            ParticipationStatus.PENDING,
            ParticipationStatus.CONFIRMED
    );

    private final EventRegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final UserService userService;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public EventRegistrationResponse register(String eventId) {
        UUID eventUuid = UUID.fromString(eventId);
        UUID volunteerId = currentUserService.requireUserId();

        Event event = eventRepository.findById(eventUuid)
                .orElseThrow(() -> new IllegalArgumentException("event not found"));
        User volunteer = userService.findById(volunteerId);
        if (volunteer == null) {
            throw new IllegalArgumentException("volunteer not found");
        }
        if (event.getOwner().getId().equals(volunteerId)) {
            throw new IllegalArgumentException("event owner cannot register as volunteer");
        }

        registrationRepository.findByEventIdAndVolunteerId(eventUuid, volunteerId)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("volunteer is already registered for this event");
                });

        if (event.getCapacity() != null) {
            long occupiedSpots = registrationRepository.countByEventIdAndStatusIn(eventUuid, CAPACITY_STATUSES);
            if (occupiedSpots >= event.getCapacity()) {
                throw new IllegalArgumentException("event capacity has been reached");
            }
        }

        EventRegistration registration = EventRegistration.builder()
                .event(event)
                .volunteer(volunteer)
                .status(ParticipationStatus.PENDING)
                .statusUpdatedAt(Instant.now())
                .build();

        return EventRegistrationResponse.from(registrationRepository.save(registration));
    }

    @Override
    @Transactional
    public EventRegistrationResponse confirm(String registrationId) {
        EventRegistration registration = findRegistration(registrationId);
        ensureCurrentUserOwnsEvent(registration);
        ensureStatus(registration, ParticipationStatus.PENDING, "only pending registrations can be confirmed");

        registration.setStatus(ParticipationStatus.CONFIRMED);
        registration.setStatusUpdatedAt(Instant.now());
        return EventRegistrationResponse.from(registrationRepository.save(registration));
    }

    @Override
    @Transactional
    public EventRegistrationResponse reject(String registrationId, EventRegistrationDecisionRequest request) {
        EventRegistration registration = findRegistration(registrationId);
        ensureCurrentUserOwnsEvent(registration);
        ensureStatus(registration, ParticipationStatus.PENDING, "only pending registrations can be rejected");

        registration.setStatus(ParticipationStatus.REJECTED);
        registration.setJustification(readJustification(request));
        registration.setStatusUpdatedAt(Instant.now());
        return EventRegistrationResponse.from(registrationRepository.save(registration));
    }

    @Override
    @Transactional
    public EventRegistrationResponse dismiss(String registrationId, EventRegistrationDecisionRequest request) {
        EventRegistration registration = findRegistration(registrationId);
        ensureCurrentUserOwnsEvent(registration);
        ensureStatus(registration, ParticipationStatus.CONFIRMED, "only confirmed registrations can be dismissed");

        registration.setStatus(ParticipationStatus.DISMISSED);
        registration.setJustification(readJustification(request));
        registration.setStatusUpdatedAt(Instant.now());
        return EventRegistrationResponse.from(registrationRepository.save(registration));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventRegistrationResponse> listByEvent(String eventId) {
        UUID eventUuid = UUID.fromString(eventId);
        Event event = eventRepository.findById(eventUuid)
                .orElseThrow(() -> new IllegalArgumentException("event not found"));
        ensureCurrentUserOwnsEvent(event);

        return registrationRepository.findAllByEventIdOrderByRegisteredAtAsc(eventUuid)
                .stream()
                .map(EventRegistrationResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventRegistrationResponse> listCurrentUserRegistrations() {
        UUID volunteerId = currentUserService.requireUserId();
        return registrationRepository.findAllByVolunteerIdOrderByRegisteredAtDesc(volunteerId)
                .stream()
                .map(EventRegistrationResponse::from)
                .toList();
    }

    private EventRegistration findRegistration(String registrationId) {
        return registrationRepository.findById(UUID.fromString(registrationId))
                .orElseThrow(() -> new IllegalArgumentException("registration not found"));
    }

    private void ensureCurrentUserOwnsEvent(EventRegistration registration) {
        ensureCurrentUserOwnsEvent(registration.getEvent());
    }

    private void ensureCurrentUserOwnsEvent(Event event) {
        UUID currentUserId = currentUserService.requireUserId();
        if (!event.getOwner().getId().equals(currentUserId)) {
            throw new IllegalArgumentException("only the event owner can manage registrations");
        }
    }

    private void ensureStatus(EventRegistration registration, ParticipationStatus expected, String message) {
        if (registration.getStatus() != expected) {
            throw new IllegalArgumentException(message);
        }
    }

    private String readJustification(EventRegistrationDecisionRequest request) {
        if (request == null || request.justification() == null || request.justification().isBlank()) {
            return null;
        }
        return request.justification().trim();
    }
}
