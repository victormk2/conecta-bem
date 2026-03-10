package br.com.conectabem.service;

import br.com.conectabem.dto.event.EventRegistrationDTO;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    private EventRegistrationServiceImpl registrationService;

    @BeforeEach
    void setup() {
        registrationService = new EventRegistrationServiceImpl(eventRepository, registrationRepository, userRepository);
    }

    @Test
    void registerCreatesNewParticipationWhenEventHasCapacity() {
        UUID eventId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
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
        User volunteer = User.builder()
                .id(volunteerId)
                .email("vol@example.com")
                .fullName("Volunteer")
                .password("HASH")
                .role(UserRole.USER.name())
                .createdAt(Instant.now())
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(registrationRepository.findByEventIdAndVolunteerId(eventId, volunteerId)).thenReturn(Optional.empty());
        when(registrationRepository.countByEventIdAndStatusIn(eq(eventId), any(Set.class))).thenReturn(1L);
        when(registrationRepository.save(any(EventRegistration.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(volunteerId)).thenReturn(Optional.of(volunteer));

        EventRegistrationDTO created = registrationService.register(eventId, volunteerId);

        assertEquals(ParticipationStatus.REGISTERED, created.getStatus());
        assertEquals("Volunteer", created.getVolunteerName());
    }

    @Test
    void registerRejectsWhenVolunteerAlreadyRegistered() {
        UUID eventId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
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

        assertThrows(IllegalArgumentException.class, () -> registrationService.register(eventId, volunteerId));
    }

    @Test
    void registerRestoresCanceledRegistration() {
        UUID eventId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        Event event = Event.builder()
                .id(eventId)
                .ownerId(UUID.randomUUID())
                .title("Evento")
                .location("Sao Paulo")
                .activityType("Limpeza")
                .startsAt(Instant.now().plusSeconds(3600))
                .capacity(5)
                .createdAt(Instant.now())
                .build();
        EventRegistration existing = EventRegistration.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .volunteerId(volunteerId)
                .status(ParticipationStatus.CANCELED)
                .registeredAt(Instant.now())
                .justification("old")
                .build();
        User volunteer = User.builder()
                .id(volunteerId)
                .email("vol@example.com")
                .fullName("Volunteer")
                .password("HASH")
                .role(UserRole.USER.name())
                .createdAt(Instant.now())
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(registrationRepository.findByEventIdAndVolunteerId(eventId, volunteerId)).thenReturn(Optional.of(existing));
        when(registrationRepository.countByEventIdAndStatusIn(eq(eventId), any(Set.class))).thenReturn(1L);
        when(registrationRepository.save(any(EventRegistration.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(volunteerId)).thenReturn(Optional.of(volunteer));

        EventRegistrationDTO restored = registrationService.register(eventId, volunteerId);

        assertEquals(ParticipationStatus.REGISTERED, restored.getStatus());
    }

    @Test
    void cancelRegistrationRejectsSameDayCancellation() {
        UUID eventId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
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
                .status(ParticipationStatus.REGISTERED)
                .registeredAt(Instant.now())
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(registrationRepository.findByEventIdAndVolunteerId(eventId, volunteerId)).thenReturn(Optional.of(registration));

        assertThrows(IllegalArgumentException.class, () -> registrationService.cancelRegistration(eventId, volunteerId));
        verify(registrationRepository, never()).save(any(EventRegistration.class));
    }

    @Test
    void cancelRegistrationReturnsFalseWhenAlreadyCanceled() {
        UUID eventId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        Event event = Event.builder()
                .id(eventId)
                .ownerId(UUID.randomUUID())
                .title("Evento")
                .location("Sao Paulo")
                .activityType("Limpeza")
                .startsAt(Instant.now().plusSeconds(86400 * 2))
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

        boolean canceled = registrationService.cancelRegistration(eventId, volunteerId);

        assertFalse(canceled);
    }

    @Test
    void updateParticipationStatusAcceptsAbsentAfterEventStart() {
        UUID ownerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
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
        User volunteer = User.builder()
                .id(volunteerId)
                .email("vol@example.com")
                .fullName("Volunteer")
                .password("HASH")
                .role(UserRole.USER.name())
                .createdAt(Instant.now())
                .build();
        UpdateParticipationStatusRequest request = new UpdateParticipationStatusRequest();
        request.setStatus(ParticipationStatus.ABSENT);

        when(eventRepository.findByIdAndOwnerId(eventId, ownerId)).thenReturn(Optional.of(event));
        when(registrationRepository.findByEventIdAndVolunteerId(eventId, volunteerId)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(any(EventRegistration.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(volunteerId)).thenReturn(Optional.of(volunteer));

        EventRegistrationDTO updated = registrationService.updateParticipationStatus(eventId, volunteerId, request, ownerId);

        assertEquals(ParticipationStatus.ABSENT, updated.getStatus());
    }

    @Test
    void justifyAbsencePromotesAbsentRegistrationToJustified() {
        UUID eventId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
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
        User volunteer = User.builder()
                .id(volunteerId)
                .email("vol@example.com")
                .fullName("Volunteer")
                .password("HASH")
                .role(UserRole.USER.name())
                .createdAt(Instant.now())
                .build();
        JustifyAbsenceRequest request = new JustifyAbsenceRequest();
        request.setJustification("Imprevisto familiar");

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(registrationRepository.findByEventIdAndVolunteerId(eventId, volunteerId)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(any(EventRegistration.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(volunteerId)).thenReturn(Optional.of(volunteer));

        EventRegistrationDTO updated = registrationService.justifyAbsence(eventId, volunteerId, request);

        assertEquals(ParticipationStatus.JUSTIFIED, updated.getStatus());
        assertEquals("Imprevisto familiar", updated.getJustification());
    }

    @Test
    void justifyAbsenceRejectsWhenRegistrationIsNotAbsent() {
        UUID eventId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
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
        JustifyAbsenceRequest request = new JustifyAbsenceRequest();
        request.setJustification("Motivo");

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(registrationRepository.findByEventIdAndVolunteerId(eventId, volunteerId)).thenReturn(Optional.of(registration));

        assertThrows(IllegalArgumentException.class, () -> registrationService.justifyAbsence(eventId, volunteerId, request));
    }

    @Test
    void listVolunteerHistoryCanFilterFutureEvents() {
        UUID volunteerId = UUID.randomUUID();
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
        User volunteer = User.builder()
                .id(volunteerId)
                .email("vol@example.com")
                .fullName("Volunteer")
                .password("HASH")
                .role(UserRole.USER.name())
                .createdAt(Instant.now())
                .build();

        when(registrationRepository.findAllByVolunteerIdOrderByRegisteredAtDesc(volunteerId))
                .thenReturn(List.of(futureRegistration, pastRegistration));
        when(eventRepository.findById(futureEventId)).thenReturn(Optional.of(Event.builder()
                .id(futureEventId)
                .ownerId(UUID.randomUUID())
                .title("Future")
                .location("Sao Paulo")
                .activityType("Mutirao")
                .startsAt(Instant.now().plusSeconds(3600))
                .createdAt(Instant.now())
                .build()));
        when(eventRepository.findById(pastEventId)).thenReturn(Optional.of(Event.builder()
                .id(pastEventId)
                .ownerId(UUID.randomUUID())
                .title("Past")
                .location("Sao Paulo")
                .activityType("Mutirao")
                .startsAt(Instant.now().minusSeconds(3600))
                .createdAt(Instant.now())
                .build()));
        when(userRepository.findById(volunteerId)).thenReturn(Optional.of(volunteer));

        List<EventRegistrationDTO> history = registrationService.listVolunteerHistory(volunteerId, true);

        assertEquals(1, history.size());
        assertEquals(futureEventId, history.getFirst().getEventId());
        assertTrue(history.getFirst().getVolunteerEmail().contains("@"));
    }
}
