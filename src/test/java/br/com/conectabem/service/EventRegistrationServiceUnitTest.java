package br.com.conectabem.service;

import br.com.conectabem.dto.event.JustifyAbsenceRequest;
import br.com.conectabem.dto.event.UpdateParticipationStatusRequest;
import br.com.conectabem.model.Event;
import br.com.conectabem.model.EventRegistration;
import br.com.conectabem.model.ParticipationStatus;
import br.com.conectabem.model.User;
import br.com.conectabem.model.UserRole;
import br.com.conectabem.repository.EventRegistrationRepository;
import br.com.conectabem.repository.EventRepository;
import br.com.conectabem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventRegistrationServiceUnitTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventRegistrationRepository registrationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserService currentUserService;

    private EventRegistrationServiceImpl registrationService;

    @BeforeEach
    void setup() {
        registrationService = new EventRegistrationServiceImpl(eventRepository, registrationRepository, userRepository, currentUserService);
    }

    @Test
    void registerCreatesNewParticipationWhenEventHasCapacity() {
        UUID eventId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        when(currentUserService.requireUserId()).thenReturn(volunteerId);

        Event event = Event.builder()
                .id(eventId)
                .ownerId(UUID.randomUUID())
                .title("A")
                .location("Sao Paulo")
                .activityType("Mutirao")
                .startsAt(Instant.now().plusSeconds(3600))
                .capacity(2)
                .createdAt(Instant.now())
                .build();

        User volunteer = new User();
        volunteer.setId(volunteerId);
        volunteer.setUsername("vol");
        volunteer.setEmail("vol@example.com");
        volunteer.setFullName("Volunteer");
        volunteer.setRole(UserRole.USER);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(registrationRepository.findByEventIdAndVolunteerId(eventId, volunteerId)).thenReturn(Optional.empty());
        when(registrationRepository.countByEventIdAndStatusIn(eq(eventId), any(Set.class))).thenReturn(1L);
        when(registrationRepository.save(any(EventRegistration.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(volunteerId)).thenReturn(Optional.of(volunteer));

        var created = registrationService.register(eventId);

        assertNotNull(created.getId());
        assertEquals(ParticipationStatus.REGISTERED, created.getStatus());
        assertEquals("Volunteer", created.getVolunteerName());
    }

    @Test
    void registerRejectsWhenEventIsFull() {
        UUID eventId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        when(currentUserService.requireUserId()).thenReturn(volunteerId);

        Event event = Event.builder()
                .id(eventId)
                .ownerId(UUID.randomUUID())
                .title("A")
                .location("Sao Paulo")
                .activityType("Mutirao")
                .startsAt(Instant.now().plusSeconds(3600))
                .capacity(1)
                .createdAt(Instant.now())
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(registrationRepository.findByEventIdAndVolunteerId(eventId, volunteerId)).thenReturn(Optional.empty());
        when(registrationRepository.countByEventIdAndStatusIn(eq(eventId), any(Set.class))).thenReturn(1L);

        assertThrows(IllegalArgumentException.class, () -> registrationService.register(eventId));
    }

    @Test
    void registerRejectsWhenOwnerTriesToRegister() {
        UUID eventId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        when(currentUserService.requireUserId()).thenReturn(ownerId);

        Event event = Event.builder()
                .id(eventId)
                .ownerId(ownerId)
                .title("A")
                .location("Sao Paulo")
                .activityType("Mutirao")
                .startsAt(Instant.now().plusSeconds(3600))
                .createdAt(Instant.now())
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        assertThrows(IllegalArgumentException.class, () -> registrationService.register(eventId));
    }

    @Test
    void registerRejectsWhenEventHasAlreadyStarted() {
        UUID eventId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        when(currentUserService.requireUserId()).thenReturn(volunteerId);

        Event event = Event.builder()
                .id(eventId)
                .ownerId(UUID.randomUUID())
                .title("A")
                .location("Sao Paulo")
                .activityType("Mutirao")
                .startsAt(Instant.now().minusSeconds(5))
                .createdAt(Instant.now())
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        assertThrows(IllegalArgumentException.class, () -> registrationService.register(eventId));
    }

    @Test
    void registerRejectsWhenVolunteerAlreadyRegistered() {
        UUID eventId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        when(currentUserService.requireUserId()).thenReturn(volunteerId);

        Event event = Event.builder()
                .id(eventId)
                .ownerId(UUID.randomUUID())
                .title("Evento")
                .location("Sao Paulo")
                .activityType("Limpeza")
                .startsAt(Instant.now().plusSeconds(3600))
                .createdAt(Instant.now())
                .build();
        EventRegistration existing = EventRegistration.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .volunteerId(volunteerId)
                .status(ParticipationStatus.REGISTERED)
                .registeredAt(Instant.now())
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(registrationRepository.findByEventIdAndVolunteerId(eventId, volunteerId)).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class, () -> registrationService.register(eventId));
    }

    @Test
    void registerRestoresCanceledRegistration() {
        UUID eventId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        when(currentUserService.requireUserId()).thenReturn(volunteerId);

        Event event = Event.builder()
                .id(eventId)
                .ownerId(UUID.randomUUID())
                .title("A")
                .location("Sao Paulo")
                .activityType("Mutirao")
                .startsAt(Instant.now().plusSeconds(3600))
                .capacity(10)
                .createdAt(Instant.now())
                .build();

        EventRegistration canceled = EventRegistration.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .volunteerId(volunteerId)
                .status(ParticipationStatus.CANCELED)
                .registeredAt(Instant.now().minusSeconds(7200))
                .statusUpdatedAt(Instant.now().minusSeconds(7200))
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(registrationRepository.findByEventIdAndVolunteerId(eventId, volunteerId)).thenReturn(Optional.of(canceled));
        when(registrationRepository.countByEventIdAndStatusIn(eq(eventId), any(Set.class))).thenReturn(0L);
        when(registrationRepository.save(any(EventRegistration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var restored = registrationService.register(eventId);

        assertEquals(ParticipationStatus.REGISTERED, restored.getStatus());
        verify(registrationRepository).save(any(EventRegistration.class));
    }

    @Test
    void registerDoesNotCheckCapacityWhenUnlimited() {
        UUID eventId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        when(currentUserService.requireUserId()).thenReturn(volunteerId);

        Event event = Event.builder()
                .id(eventId)
                .ownerId(UUID.randomUUID())
                .title("A")
                .location("Sao Paulo")
                .activityType("Mutirao")
                .startsAt(Instant.now().plusSeconds(3600))
                .capacity(null)
                .createdAt(Instant.now())
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(registrationRepository.findByEventIdAndVolunteerId(eventId, volunteerId)).thenReturn(Optional.empty());
        when(registrationRepository.save(any(EventRegistration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        registrationService.register(eventId);

        verify(registrationRepository, never()).countByEventIdAndStatusIn(eq(eventId), any(Set.class));
    }

    @Test
    void cancelRegistrationMarksCanceledWhenAllowed() {
        UUID eventId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        when(currentUserService.requireUserId()).thenReturn(volunteerId);

        Event event = Event.builder()
                .id(eventId)
                .ownerId(UUID.randomUUID())
                .title("A")
                .location("Sao Paulo")
                .activityType("Mutirao")
                .startsAt(Instant.now().plusSeconds(24 * 3600))
                .createdAt(Instant.now())
                .build();

        EventRegistration registration = EventRegistration.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .volunteerId(volunteerId)
                .status(ParticipationStatus.REGISTERED)
                .registeredAt(Instant.now())
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(registrationRepository.findByEventIdAndVolunteerId(eventId, volunteerId)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(any(EventRegistration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean canceled = registrationService.cancelRegistration(eventId);

        assertTrue(canceled);
        assertEquals(ParticipationStatus.CANCELED, registration.getStatus());
        verify(registrationRepository).save(any(EventRegistration.class));
    }

    @Test
    void cancelRegistrationThrowsWhenRegistrationDoesNotExist() {
        UUID eventId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        when(currentUserService.requireUserId()).thenReturn(volunteerId);

        Event event = Event.builder()
                .id(eventId)
                .ownerId(UUID.randomUUID())
                .title("A")
                .location("Sao Paulo")
                .activityType("Mutirao")
                .startsAt(Instant.now().plusSeconds(24 * 3600))
                .createdAt(Instant.now())
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(registrationRepository.findByEventIdAndVolunteerId(eventId, volunteerId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> registrationService.cancelRegistration(eventId));
    }

    @Test
    void cancelRegistrationRejectsWhenStatusIsPresent() {
        UUID eventId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        when(currentUserService.requireUserId()).thenReturn(volunteerId);

        Event event = Event.builder()
                .id(eventId)
                .ownerId(UUID.randomUUID())
                .title("A")
                .location("Sao Paulo")
                .activityType("Mutirao")
                .startsAt(Instant.now().plusSeconds(24 * 3600))
                .createdAt(Instant.now())
                .build();

        EventRegistration registration = EventRegistration.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .volunteerId(volunteerId)
                .status(ParticipationStatus.PRESENT)
                .registeredAt(Instant.now())
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(registrationRepository.findByEventIdAndVolunteerId(eventId, volunteerId)).thenReturn(Optional.of(registration));

        assertThrows(IllegalArgumentException.class, () -> registrationService.cancelRegistration(eventId));
    }

    @Test
    void cancelRegistrationReturnsFalseWhenAlreadyCanceled() {
        UUID eventId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        when(currentUserService.requireUserId()).thenReturn(volunteerId);

        Event event = Event.builder()
                .id(eventId)
                .ownerId(UUID.randomUUID())
                .title("A")
                .location("Sao Paulo")
                .activityType("Mutirao")
                .startsAt(Instant.now().plusSeconds(24 * 3600))
                .createdAt(Instant.now())
                .build();

        EventRegistration registration = EventRegistration.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .volunteerId(volunteerId)
                .status(ParticipationStatus.CANCELED)
                .registeredAt(Instant.now())
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(registrationRepository.findByEventIdAndVolunteerId(eventId, volunteerId)).thenReturn(Optional.of(registration));

        boolean canceled = registrationService.cancelRegistration(eventId);

        assertFalse(canceled);
        verify(registrationRepository, never()).save(any(EventRegistration.class));
    }

    @Test
    void cancelRegistrationRejectsOnTheSameDayOfTheEvent() {
        UUID eventId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        when(currentUserService.requireUserId()).thenReturn(volunteerId);

        ZoneId zone = ZoneId.systemDefault();
        Instant startsAt = LocalDate.now(zone).atStartOfDay(zone).plusHours(12).toInstant();
        Event event = Event.builder()
                .id(eventId)
                .ownerId(UUID.randomUUID())
                .title("A")
                .location("Sao Paulo")
                .activityType("Mutirao")
                .startsAt(startsAt)
                .createdAt(Instant.now())
                .build();

        EventRegistration registration = EventRegistration.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .volunteerId(volunteerId)
                .status(ParticipationStatus.REGISTERED)
                .registeredAt(Instant.now())
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(registrationRepository.findByEventIdAndVolunteerId(eventId, volunteerId)).thenReturn(Optional.of(registration));

        assertThrows(IllegalArgumentException.class, () -> registrationService.cancelRegistration(eventId));
    }

    @Test
    void updateParticipationStatusRejectsBeforeEventStarts() {
        UUID eventId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        when(currentUserService.requireUserId()).thenReturn(ownerId);

        Event event = Event.builder()
                .id(eventId)
                .ownerId(ownerId)
                .title("A")
                .location("Sao Paulo")
                .activityType("Mutirao")
                .startsAt(Instant.now().plusSeconds(3600))
                .createdAt(Instant.now())
                .build();
        EventRegistration registration = EventRegistration.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .volunteerId(volunteerId)
                .status(ParticipationStatus.REGISTERED)
                .registeredAt(Instant.now())
                .build();
        UpdateParticipationStatusRequest request = new UpdateParticipationStatusRequest();
        request.setStatus(ParticipationStatus.PRESENT);

        when(eventRepository.findByIdAndOwnerId(eventId, ownerId)).thenReturn(Optional.of(event));
        when(registrationRepository.findByEventIdAndVolunteerId(eventId, volunteerId)).thenReturn(Optional.of(registration));

        assertThrows(IllegalArgumentException.class, () -> registrationService.updateParticipationStatus(eventId, volunteerId, request));
    }

    @Test
    void updateParticipationStatusRejectsInvalidStatus() {
        UUID eventId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        when(currentUserService.requireUserId()).thenReturn(ownerId);

        Event event = Event.builder()
                .id(eventId)
                .ownerId(ownerId)
                .title("A")
                .location("Sao Paulo")
                .activityType("Mutirao")
                .startsAt(Instant.now().minusSeconds(3600))
                .createdAt(Instant.now())
                .build();
        EventRegistration registration = EventRegistration.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .volunteerId(volunteerId)
                .status(ParticipationStatus.REGISTERED)
                .registeredAt(Instant.now())
                .build();
        UpdateParticipationStatusRequest request = new UpdateParticipationStatusRequest();
        request.setStatus(ParticipationStatus.JUSTIFIED);

        when(eventRepository.findByIdAndOwnerId(eventId, ownerId)).thenReturn(Optional.of(event));
        when(registrationRepository.findByEventIdAndVolunteerId(eventId, volunteerId)).thenReturn(Optional.of(registration));

        assertThrows(IllegalArgumentException.class, () -> registrationService.updateParticipationStatus(eventId, volunteerId, request));
    }

    @Test
    void updateParticipationStatusUpdatesToAbsent() {
        UUID eventId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        when(currentUserService.requireUserId()).thenReturn(ownerId);

        Event event = Event.builder()
                .id(eventId)
                .ownerId(ownerId)
                .title("A")
                .location("Sao Paulo")
                .activityType("Mutirao")
                .startsAt(Instant.now().minusSeconds(3600))
                .createdAt(Instant.now())
                .build();
        EventRegistration registration = EventRegistration.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .volunteerId(volunteerId)
                .status(ParticipationStatus.REGISTERED)
                .registeredAt(Instant.now().minusSeconds(7200))
                .build();
        UpdateParticipationStatusRequest request = new UpdateParticipationStatusRequest();
        request.setStatus(ParticipationStatus.ABSENT);

        when(eventRepository.findByIdAndOwnerId(eventId, ownerId)).thenReturn(Optional.of(event));
        when(registrationRepository.findByEventIdAndVolunteerId(eventId, volunteerId)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(any(EventRegistration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var updated = registrationService.updateParticipationStatus(eventId, volunteerId, request);

        assertEquals(ParticipationStatus.ABSENT, updated.getStatus());
    }

    @Test
    void updateParticipationStatusThrowsWhenRegistrationDoesNotExist() {
        UUID eventId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        when(currentUserService.requireUserId()).thenReturn(ownerId);

        Event event = Event.builder()
                .id(eventId)
                .ownerId(ownerId)
                .title("A")
                .location("Sao Paulo")
                .activityType("Mutirao")
                .startsAt(Instant.now().minusSeconds(3600))
                .createdAt(Instant.now())
                .build();
        UpdateParticipationStatusRequest request = new UpdateParticipationStatusRequest();
        request.setStatus(ParticipationStatus.PRESENT);

        when(eventRepository.findByIdAndOwnerId(eventId, ownerId)).thenReturn(Optional.of(event));
        when(registrationRepository.findByEventIdAndVolunteerId(eventId, volunteerId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> registrationService.updateParticipationStatus(eventId, volunteerId, request));
    }

    @Test
    void justifyAbsencePromotesAbsentRegistrationToJustified() {
        UUID eventId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        when(currentUserService.requireUserId()).thenReturn(volunteerId);

        Event event = Event.builder()
                .id(eventId)
                .ownerId(UUID.randomUUID())
                .title("A")
                .location("Sao Paulo")
                .activityType("Mutirao")
                .startsAt(Instant.now().minusSeconds(3600))
                .createdAt(Instant.now())
                .build();
        EventRegistration registration = EventRegistration.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .volunteerId(volunteerId)
                .status(ParticipationStatus.ABSENT)
                .registeredAt(Instant.now().minusSeconds(7200))
                .build();
        JustifyAbsenceRequest request = new JustifyAbsenceRequest();
        request.setJustification("  Imprevisto familiar  ");

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(registrationRepository.findByEventIdAndVolunteerId(eventId, volunteerId)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(any(EventRegistration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var updated = registrationService.justifyAbsence(eventId, request);

        assertEquals(ParticipationStatus.JUSTIFIED, updated.getStatus());
        assertEquals("Imprevisto familiar", updated.getJustification());
    }

    @Test
    void justifyAbsenceRejectsWhenRegistrationIsNotAbsent() {
        UUID eventId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        when(currentUserService.requireUserId()).thenReturn(volunteerId);

        Event event = Event.builder()
                .id(eventId)
                .ownerId(UUID.randomUUID())
                .title("A")
                .location("Sao Paulo")
                .activityType("Mutirao")
                .startsAt(Instant.now().minusSeconds(3600))
                .createdAt(Instant.now())
                .build();

        EventRegistration registration = EventRegistration.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .volunteerId(volunteerId)
                .status(ParticipationStatus.PRESENT)
                .registeredAt(Instant.now())
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(registrationRepository.findByEventIdAndVolunteerId(eventId, volunteerId)).thenReturn(Optional.of(registration));

        JustifyAbsenceRequest request = new JustifyAbsenceRequest();
        request.setJustification("Motivo");

        assertThrows(IllegalArgumentException.class, () -> registrationService.justifyAbsence(eventId, request));
    }

    @Test
    void justifyAbsenceRejectsBeforeEventStarts() {
        UUID eventId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        when(currentUserService.requireUserId()).thenReturn(volunteerId);

        Event event = Event.builder()
                .id(eventId)
                .ownerId(UUID.randomUUID())
                .title("A")
                .location("Sao Paulo")
                .activityType("Mutirao")
                .startsAt(Instant.now().plusSeconds(3600))
                .createdAt(Instant.now())
                .build();

        EventRegistration registration = EventRegistration.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .volunteerId(volunteerId)
                .status(ParticipationStatus.ABSENT)
                .registeredAt(Instant.now())
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(registrationRepository.findByEventIdAndVolunteerId(eventId, volunteerId)).thenReturn(Optional.of(registration));

        JustifyAbsenceRequest request = new JustifyAbsenceRequest();
        request.setJustification("Motivo");

        assertThrows(IllegalArgumentException.class, () -> registrationService.justifyAbsence(eventId, request));
    }

    @Test
    void listParticipantsReturnsRegistrationsForOwnedEvent() {
        UUID eventId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        when(currentUserService.requireUserId()).thenReturn(ownerId);

        Event event = Event.builder()
                .id(eventId)
                .ownerId(ownerId)
                .title("A")
                .location("Sao Paulo")
                .activityType("Mutirao")
                .startsAt(Instant.now().plusSeconds(3600))
                .createdAt(Instant.now())
                .build();

        UUID volunteerId = UUID.randomUUID();
        EventRegistration registration = EventRegistration.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .volunteerId(volunteerId)
                .status(ParticipationStatus.REGISTERED)
                .registeredAt(Instant.now())
                .build();

        User volunteer = new User();
        volunteer.setId(volunteerId);
        volunteer.setUsername("vol");
        volunteer.setEmail("vol@example.com");
        volunteer.setFullName("Volunteer");
        volunteer.setRole(UserRole.USER);

        when(eventRepository.findByIdAndOwnerId(eventId, ownerId)).thenReturn(Optional.of(event));
        when(registrationRepository.findAllByEventIdOrderByRegisteredAtAsc(eventId)).thenReturn(List.of(registration));
        when(userRepository.findById(volunteerId)).thenReturn(Optional.of(volunteer));

        var participants = registrationService.listParticipants(eventId);

        assertEquals(1, participants.size());
        assertEquals("Volunteer", participants.getFirst().getVolunteerName());
    }

    @Test
    void myRegistrationsCanFilterFutureEvents() {
        UUID volunteerId = UUID.randomUUID();
        when(currentUserService.requireUserId()).thenReturn(volunteerId);

        UUID futureEventId = UUID.randomUUID();
        UUID pastEventId = UUID.randomUUID();

        EventRegistration futureRegistration = EventRegistration.builder()
                .id(UUID.randomUUID())
                .eventId(futureEventId)
                .volunteerId(volunteerId)
                .status(ParticipationStatus.REGISTERED)
                .registeredAt(Instant.now())
                .build();
        EventRegistration pastRegistration = EventRegistration.builder()
                .id(UUID.randomUUID())
                .eventId(pastEventId)
                .volunteerId(volunteerId)
                .status(ParticipationStatus.PRESENT)
                .registeredAt(Instant.now().minusSeconds(100))
                .build();

        when(registrationRepository.findAllByVolunteerIdOrderByRegisteredAtDesc(volunteerId))
                .thenReturn(List.of(futureRegistration, pastRegistration));

        when(eventRepository.findById(futureEventId)).thenReturn(Optional.of(Event.builder()
                .id(futureEventId)
                .ownerId(UUID.randomUUID())
                .title("F")
                .location("Sao Paulo")
                .activityType("Mutirao")
                .startsAt(Instant.now().plusSeconds(3600))
                .createdAt(Instant.now())
                .build()));
        when(eventRepository.findById(pastEventId)).thenReturn(Optional.of(Event.builder()
                .id(pastEventId)
                .ownerId(UUID.randomUUID())
                .title("P")
                .location("Sao Paulo")
                .activityType("Mutirao")
                .startsAt(Instant.now().minusSeconds(3600))
                .createdAt(Instant.now())
                .build()));

        User volunteer = new User();
        volunteer.setId(volunteerId);
        volunteer.setUsername("vol");
        volunteer.setEmail("vol@example.com");
        volunteer.setFullName("Volunteer");
        volunteer.setRole(UserRole.USER);
        when(userRepository.findById(volunteerId)).thenReturn(Optional.of(volunteer));

        var futureOnly = registrationService.myRegistrations(true);

        assertEquals(1, futureOnly.size());
        assertEquals(futureEventId, futureOnly.getFirst().getEventId());
    }

    @Test
    void myRegistrationsReturnsAllWhenFutureOnlyIsFalse() {
        UUID volunteerId = UUID.randomUUID();
        when(currentUserService.requireUserId()).thenReturn(volunteerId);

        EventRegistration r1 = EventRegistration.builder()
                .id(UUID.randomUUID())
                .eventId(UUID.randomUUID())
                .volunteerId(volunteerId)
                .status(ParticipationStatus.REGISTERED)
                .registeredAt(Instant.now())
                .build();
        EventRegistration r2 = EventRegistration.builder()
                .id(UUID.randomUUID())
                .eventId(UUID.randomUUID())
                .volunteerId(volunteerId)
                .status(ParticipationStatus.CANCELED)
                .registeredAt(Instant.now())
                .build();

        when(registrationRepository.findAllByVolunteerIdOrderByRegisteredAtDesc(volunteerId)).thenReturn(List.of(r1, r2));

        User volunteer = new User();
        volunteer.setId(volunteerId);
        volunteer.setUsername("vol");
        volunteer.setEmail("vol@example.com");
        volunteer.setFullName("Volunteer");
        volunteer.setRole(UserRole.USER);
        when(userRepository.findById(volunteerId)).thenReturn(Optional.of(volunteer));

        var all = registrationService.myRegistrations(false);

        assertEquals(2, all.size());
    }
}
