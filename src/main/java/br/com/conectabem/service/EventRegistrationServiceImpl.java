package br.com.conectabem.service;

import br.com.conectabem.dto.event.EventRegistrationDTO;
import br.com.conectabem.dto.event.JustifyAbsenceRequest;
import br.com.conectabem.dto.event.UpdateParticipationStatusRequest;
import br.com.conectabem.model.Event;
import br.com.conectabem.model.EventRegistration;
import br.com.conectabem.model.ParticipationStatus;
import br.com.conectabem.model.User;
import br.com.conectabem.repository.EventRegistrationRepository;
import br.com.conectabem.repository.EventRepository;
import br.com.conectabem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventRegistrationServiceImpl implements EventRegistrationService {

    private static final Set<ParticipationStatus> OCCUPYING_STATUSES = Set.of(
            ParticipationStatus.REGISTERED,
            ParticipationStatus.PRESENT,
            ParticipationStatus.ABSENT,
            ParticipationStatus.JUSTIFIED
    );

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    @Override
    public EventRegistrationDTO register(UUID eventId) {
        UUID volunteerId = currentUserService.requireUserId();
        Event event = loadEvent(eventId);
        if (event.getOwnerId().equals(volunteerId)) {
            throw new IllegalArgumentException("event owner cannot register as volunteer");
        }
        if (event.getStartsAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("event has already started");
        }

        EventRegistration registration = registrationRepository.findByEventIdAndVolunteerId(eventId, volunteerId)
                .map(existing -> restoreCanceledRegistration(existing, event))
                .orElseGet(() -> createRegistration(event, volunteerId));

        return toRegistrationDTO(registration);
    }

    @Override
    public boolean cancelRegistration(UUID eventId) {
        UUID volunteerId = currentUserService.requireUserId();
        Event event = loadEvent(eventId);
        EventRegistration registration = registrationRepository.findByEventIdAndVolunteerId(eventId, volunteerId)
                .orElseThrow(() -> new IllegalArgumentException("registration not found"));

        if (!canCancel(event)) {
            throw new IllegalArgumentException("registration can only be canceled until the day before the event");
        }
        if (registration.getStatus() == ParticipationStatus.PRESENT) {
            throw new IllegalArgumentException("present registrations cannot be canceled");
        }
        if (registration.getStatus() == ParticipationStatus.CANCELED) {
            return false;
        }

        registration.setStatus(ParticipationStatus.CANCELED);
        registration.setStatusUpdatedAt(Instant.now());
        registrationRepository.save(registration);
        return true;
    }

    @Override
    public List<EventRegistrationDTO> listParticipants(UUID eventId) {
        Event event = getOwnedEvent(eventId, currentUserService.requireUserId());
        return registrationRepository.findAllByEventIdOrderByRegisteredAtAsc(event.getId())
                .stream()
                .map(this::toRegistrationDTO)
                .toList();
    }

    @Override
    public List<EventRegistrationDTO> myRegistrations(boolean futureOnly) {
        UUID volunteerId = currentUserService.requireUserId();
        Instant now = Instant.now();
        return registrationRepository.findAllByVolunteerIdOrderByRegisteredAtDesc(volunteerId)
                .stream()
                .filter(registration -> !futureOnly || loadEvent(registration.getEventId())
                        .getStartsAt()
                        .isAfter(now))
                .map(this::toRegistrationDTO)
                .toList();
    }

    @Override
    public EventRegistrationDTO updateParticipationStatus(UUID eventId,
                                                          UUID volunteerId,
                                                          UpdateParticipationStatusRequest request) {
        Event event = getOwnedEvent(eventId, currentUserService.requireUserId());
        EventRegistration registration = registrationRepository.findByEventIdAndVolunteerId(eventId, volunteerId)
                .orElseThrow(() -> new IllegalArgumentException("registration not found"));

        if (!hasStarted(event)) {
            throw new IllegalArgumentException("cannot register participation before the event happens");
        }
        if (request.getStatus() != ParticipationStatus.PRESENT && request.getStatus() != ParticipationStatus.ABSENT) {
            throw new IllegalArgumentException("status must be PRESENT or ABSENT");
        }

        registration.setStatus(request.getStatus());
        registration.setStatusUpdatedAt(Instant.now());
        registrationRepository.save(registration);
        return toRegistrationDTO(registration);
    }

    @Override
    public EventRegistrationDTO justifyAbsence(UUID eventId,
                                               JustifyAbsenceRequest request) {
        UUID volunteerId = currentUserService.requireUserId();
        Event event = loadEvent(eventId);
        EventRegistration registration = registrationRepository.findByEventIdAndVolunteerId(eventId, volunteerId)
                .orElseThrow(() -> new IllegalArgumentException("registration not found"));

        if (!hasStarted(event)) {
            throw new IllegalArgumentException("absence can only be justified after the event happens");
        }
        if (registration.getStatus() != ParticipationStatus.ABSENT) {
            throw new IllegalArgumentException("only absent registrations can be justified");
        }

        registration.setStatus(ParticipationStatus.JUSTIFIED);
        registration.setJustification(request.getJustification().trim());
        registration.setStatusUpdatedAt(Instant.now());
        registrationRepository.save(registration);
        return toRegistrationDTO(registration);
    }

    private EventRegistration createRegistration(Event event, UUID volunteerId) {
        ensureCapacity(event);

        EventRegistration registration = EventRegistration.builder()
                .id(UUID.randomUUID())
                .eventId(event.getId())
                .volunteerId(volunteerId)
                .status(ParticipationStatus.REGISTERED)
                .registeredAt(Instant.now())
                .statusUpdatedAt(Instant.now())
                .build();
        return registrationRepository.save(registration);
    }

    private EventRegistration restoreCanceledRegistration(EventRegistration registration, Event event) {
        if (registration.getStatus() != ParticipationStatus.CANCELED) {
            throw new IllegalArgumentException("user is already registered for this event");
        }

        ensureCapacity(event);
        registration.setStatus(ParticipationStatus.REGISTERED);
        registration.setJustification(null);
        registration.setStatusUpdatedAt(Instant.now());
        return registrationRepository.save(registration);
    }

    private void ensureCapacity(Event event) {
        if (event.getCapacity() == null) {
            return;
        }

        long activeRegistrations = registrationRepository.countByEventIdAndStatusIn(event.getId(), OCCUPYING_STATUSES);
        if (activeRegistrations >= event.getCapacity()) {
            throw new IllegalArgumentException("event is full");
        }
    }

    private Event getOwnedEvent(UUID eventId, UUID ownerId) {
        return eventRepository.findByIdAndOwnerId(eventId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("event not found for owner"));
    }

    private Event loadEvent(UUID eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("event not found"));
    }

    private boolean canCancel(Event event) {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        LocalDate eventDate = event.getStartsAt().atZone(ZoneId.systemDefault()).toLocalDate();
        return today.isBefore(eventDate);
    }

    private boolean hasStarted(Event event) {
        return !Instant.now().isBefore(event.getStartsAt());
    }

    private EventRegistrationDTO toRegistrationDTO(EventRegistration registration) {
        User volunteer = userRepository.findById(registration.getVolunteerId()).orElse(null);
        EventRegistrationDTO dto = new EventRegistrationDTO();
        dto.setId(registration.getId());
        dto.setEventId(registration.getEventId());
        dto.setVolunteerId(registration.getVolunteerId());
        dto.setVolunteerName(volunteer != null ? volunteer.getFullName() : null);
        dto.setVolunteerEmail(volunteer != null ? volunteer.getEmail() : null);
        dto.setStatus(registration.getStatus());
        dto.setJustification(registration.getJustification());
        dto.setRegisteredAt(registration.getRegisteredAt());
        dto.setStatusUpdatedAt(registration.getStatusUpdatedAt());
        return dto;
    }
}
