package br.com.conectabem.service;

import br.com.conectabem.dto.event.CreateEventRequest;
import br.com.conectabem.dto.event.UpdateEventRequest;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceUnitTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventRegistrationRepository registrationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserService currentUserService;

    private EventServiceImpl eventService;

    @BeforeEach
    void setup() {
        eventService = new EventServiceImpl(eventRepository, registrationRepository, userRepository, currentUserService);
    }

    @Test
    void createSavesEventWithOwnerFromCurrentUser() {
        UUID ownerId = UUID.randomUUID();
        when(currentUserService.requireUserId()).thenReturn(ownerId);
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateEventRequest request = new CreateEventRequest();
        request.setTitle("Community Meetup");
        request.setDescription("Tech and jobs");
        request.setLocation("Sao Paulo");
        request.setActivityType("Workshop");
        request.setStartsAt(Instant.parse("2026-03-10T10:00:00Z"));
        request.setEndsAt(Instant.parse("2026-03-10T12:00:00Z"));
        request.setCapacity(20);

        Event created = eventService.create(request);

        assertNotNull(created.getId());
        assertEquals(ownerId, created.getOwnerId());
        assertEquals("Workshop", created.getActivityType());
        assertEquals(20, created.getCapacity());
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void createRejectsWhenEndsBeforeStarts() {
        CreateEventRequest request = new CreateEventRequest();
        request.setTitle("A");
        request.setLocation("Sao Paulo");
        request.setActivityType("Mutirao");
        request.setStartsAt(Instant.parse("2026-03-10T12:00:00Z"));
        request.setEndsAt(Instant.parse("2026-03-10T10:00:00Z"));

        assertThrows(IllegalArgumentException.class, () -> eventService.create(request));
    }

    @Test
    void findAvailableAppliesAllFilters() {
        Event matching = Event.builder()
                .id(UUID.randomUUID())
                .ownerId(UUID.randomUUID())
                .title("Mutirao")
                .location("Sao Paulo - Centro")
                .activityType("Limpeza")
                .startsAt(Instant.parse("2026-03-20T10:00:00Z"))
                .createdAt(Instant.now())
                .build();
        Event wrongLocation = Event.builder()
                .id(UUID.randomUUID())
                .ownerId(UUID.randomUUID())
                .title("Mutirao")
                .location("Campinas")
                .activityType("Limpeza")
                .startsAt(Instant.parse("2026-03-20T10:00:00Z"))
                .createdAt(Instant.now())
                .build();
        Event wrongActivity = Event.builder()
                .id(UUID.randomUUID())
                .ownerId(UUID.randomUUID())
                .title("Mutirao")
                .location("Sao Paulo")
                .activityType("Doacao")
                .startsAt(Instant.parse("2026-03-20T10:00:00Z"))
                .createdAt(Instant.now())
                .build();
        Event beforeFromDate = Event.builder()
                .id(UUID.randomUUID())
                .ownerId(UUID.randomUUID())
                .title("Mutirao")
                .location("Sao Paulo")
                .activityType("Limpeza")
                .startsAt(Instant.parse("2026-03-10T10:00:00Z"))
                .createdAt(Instant.now())
                .build();

        when(eventRepository.findAllByOrderByStartsAtAsc())
                .thenReturn(List.of(matching, wrongLocation, wrongActivity, beforeFromDate));

        List<Event> filtered = eventService.findAvailable("sao paulo", "limpeza", "2026-03-19");

        assertEquals(1, filtered.size());
        assertEquals(matching.getId(), filtered.getFirst().getId());
    }

    @Test
    void listMineDelegatesToRepositoryWithCurrentUser() {
        UUID ownerId = UUID.randomUUID();
        when(currentUserService.requireUserId()).thenReturn(ownerId);

        Event event = Event.builder()
                .id(UUID.randomUUID())
                .ownerId(ownerId)
                .title("A")
                .location("Sao Paulo")
                .activityType("Mutirao")
                .startsAt(Instant.now())
                .createdAt(Instant.now())
                .build();
        when(eventRepository.findAllByOwnerIdOrderByStartsAtAsc(ownerId)).thenReturn(List.of(event));

        List<Event> mine = eventService.listMine();

        assertEquals(1, mine.size());
        assertEquals(ownerId, mine.getFirst().getOwnerId());
    }

    @Test
    void findOwnedByIdUsesCurrentUser() {
        UUID ownerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        when(currentUserService.requireUserId()).thenReturn(ownerId);

        Event event = Event.builder()
                .id(eventId)
                .ownerId(ownerId)
                .title("A")
                .location("Sao Paulo")
                .activityType("Mutirao")
                .startsAt(Instant.now())
                .createdAt(Instant.now())
                .build();
        when(eventRepository.findByIdAndOwnerId(eventId, ownerId)).thenReturn(Optional.of(event));

        var found = eventService.findOwnedById(eventId);

        assertTrue(found.isPresent());
        assertEquals(eventId, found.get().getId());
    }

    @Test
    void findAccessibleByIdReturnsEmptyWhenEventDoesNotExist() {
        UUID eventId = UUID.randomUUID();
        when(currentUserService.requireUserId()).thenReturn(UUID.randomUUID());
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertTrue(eventService.findAccessibleById(eventId).isEmpty());
    }

    @Test
    void updateRejectsCapacityBelowActiveRegistrations() {
        UUID ownerId = UUID.randomUUID();
        when(currentUserService.requireUserId()).thenReturn(ownerId);

        UUID eventId = UUID.randomUUID();
        Event event = Event.builder()
                .id(eventId)
                .ownerId(ownerId)
                .title("Old")
                .location("Sao Paulo")
                .activityType("Mutirao")
                .startsAt(Instant.parse("2026-03-20T10:00:00Z"))
                .capacity(10)
                .createdAt(Instant.now())
                .build();

        UpdateEventRequest request = new UpdateEventRequest();
        request.setTitle("New");
        request.setDescription("Desc");
        request.setLocation("Sao Paulo");
        request.setActivityType("Workshop");
        request.setStartsAt(Instant.parse("2026-03-21T10:00:00Z"));
        request.setEndsAt(Instant.parse("2026-03-21T12:00:00Z"));
        request.setCapacity(4);

        when(eventRepository.findByIdAndOwnerId(eventId, ownerId)).thenReturn(Optional.of(event));
        when(registrationRepository.countByEventIdAndStatusIn(eq(eventId), any(Set.class))).thenReturn(5L);

        assertThrows(IllegalArgumentException.class, () -> eventService.update(eventId, request));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void updateSavesEventWhenValid() {
        UUID ownerId = UUID.randomUUID();
        when(currentUserService.requireUserId()).thenReturn(ownerId);

        UUID eventId = UUID.randomUUID();
        Event event = Event.builder()
                .id(eventId)
                .ownerId(ownerId)
                .title("Old")
                .description("Desc")
                .location("Sao Paulo")
                .activityType("Mutirao")
                .startsAt(Instant.parse("2026-03-20T10:00:00Z"))
                .endsAt(Instant.parse("2026-03-20T12:00:00Z"))
                .capacity(10)
                .createdAt(Instant.now())
                .build();

        UpdateEventRequest request = new UpdateEventRequest();
        request.setTitle("New");
        request.setDescription("NewDesc");
        request.setLocation("Sao Paulo");
        request.setActivityType("Workshop");
        request.setStartsAt(Instant.parse("2026-03-21T10:00:00Z"));
        request.setEndsAt(Instant.parse("2026-03-21T12:00:00Z"));
        request.setCapacity(10);

        when(eventRepository.findByIdAndOwnerId(eventId, ownerId)).thenReturn(Optional.of(event));
        when(registrationRepository.countByEventIdAndStatusIn(eq(eventId), any(Set.class))).thenReturn(0L);
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var updated = eventService.update(eventId, request);

        assertTrue(updated.isPresent());
        assertEquals("New", updated.get().getTitle());
        assertEquals("NewDesc", updated.get().getDescription());
        assertEquals("Workshop", updated.get().getActivityType());
        assertNotNull(updated.get().getUpdatedAt());
    }

    @Test
    void deleteAllowsAdminToRemoveEvent() {
        UUID adminId = UUID.randomUUID();
        when(currentUserService.requireUserId()).thenReturn(adminId);

        UUID ownerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Event event = Event.builder()
                .id(eventId)
                .ownerId(ownerId)
                .title("A")
                .location("Sao Paulo")
                .activityType("Mutirao")
                .startsAt(Instant.now().plusSeconds(3600))
                .createdAt(Instant.now())
                .build();

        User admin = new User();
        admin.setId(adminId);
        admin.setUsername("admin");
        admin.setEmail("admin@example.com");
        admin.setFullName("Admin");
        admin.setRole(UserRole.ADMIN);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        boolean deleted = eventService.delete(eventId);

        assertTrue(deleted);
        verify(eventRepository).delete(event);
    }

    @Test
    void deleteAllowsOwnerToRemoveEvent() {
        UUID ownerId = UUID.randomUUID();
        when(currentUserService.requireUserId()).thenReturn(ownerId);

        UUID eventId = UUID.randomUUID();
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

        assertTrue(eventService.delete(eventId));
        verify(eventRepository).delete(event);
    }

    @Test
    void deleteReturnsFalseWhenEventDoesNotExist() {
        UUID requesterId = UUID.randomUUID();
        when(currentUserService.requireUserId()).thenReturn(requesterId);
        when(eventRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertFalse(eventService.delete(UUID.randomUUID()));
    }

    @Test
    void buildReportAggregatesCountsByStatusAndAvailableSpots() {
        UUID ownerId = UUID.randomUUID();
        when(currentUserService.requireUserId()).thenReturn(ownerId);

        UUID eventId = UUID.randomUUID();
        Event event = Event.builder()
                .id(eventId)
                .ownerId(ownerId)
                .title("A")
                .location("Sao Paulo")
                .activityType("Mutirao")
                .startsAt(Instant.now().plusSeconds(3600))
                .capacity(10)
                .createdAt(Instant.now())
                .build();

        when(eventRepository.findByIdAndOwnerId(eventId, ownerId)).thenReturn(Optional.of(event));
        when(registrationRepository.findAllByEventIdOrderByRegisteredAtAsc(eventId)).thenReturn(List.of(
                EventRegistration.builder().id(UUID.randomUUID()).eventId(eventId).volunteerId(UUID.randomUUID()).status(ParticipationStatus.REGISTERED).registeredAt(Instant.now()).build(),
                EventRegistration.builder().id(UUID.randomUUID()).eventId(eventId).volunteerId(UUID.randomUUID()).status(ParticipationStatus.PRESENT).registeredAt(Instant.now()).build(),
                EventRegistration.builder().id(UUID.randomUUID()).eventId(eventId).volunteerId(UUID.randomUUID()).status(ParticipationStatus.ABSENT).registeredAt(Instant.now()).build()
        ));
        when(registrationRepository.countByEventIdAndStatusIn(eq(eventId), any(Set.class))).thenReturn(3L);

        var report = eventService.buildReport(eventId);

        assertEquals(3, report.getTotalRegistrations());
        assertEquals(7L, report.getAvailableSpots());
        assertEquals(1L, report.getCountsByStatus().get("PRESENT"));
        assertEquals(1L, report.getCountsByStatus().get("ABSENT"));
        assertEquals(1L, report.getCountsByStatus().get("REGISTERED"));
    }

    @Test
    void buildReportThrowsWhenEventIsNotOwned() {
        UUID ownerId = UUID.randomUUID();
        when(currentUserService.requireUserId()).thenReturn(ownerId);
        when(eventRepository.findByIdAndOwnerId(any(UUID.class), eq(ownerId))).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> eventService.buildReport(UUID.randomUUID()));
    }

    @Test
    void getAvailableSpotsReturnsMinusOneWhenEventHasNoCapacity() {
        UUID eventId = UUID.randomUUID();
        Event event = Event.builder()
                .id(eventId)
                .ownerId(UUID.randomUUID())
                .title("A")
                .location("Sao Paulo")
                .activityType("Mutirao")
                .startsAt(Instant.now())
                .capacity(null)
                .createdAt(Instant.now())
                .build();
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        assertEquals(-1L, eventService.getAvailableSpots(eventId));
    }

    @Test
    void getAvailableSpotsCalculatesFromCapacityAndActiveRegistrations() {
        UUID eventId = UUID.randomUUID();
        Event event = Event.builder()
                .id(eventId)
                .ownerId(UUID.randomUUID())
                .title("A")
                .location("Sao Paulo")
                .activityType("Mutirao")
                .startsAt(Instant.now())
                .capacity(10)
                .createdAt(Instant.now())
                .build();
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(registrationRepository.countByEventIdAndStatusIn(eq(eventId), any(Set.class))).thenReturn(4L);

        assertEquals(6L, eventService.getAvailableSpots(eventId));
    }
}
