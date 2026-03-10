package br.com.conectabem.service;

import br.com.conectabem.dto.event.CreateAnnouncementRequest;
import br.com.conectabem.dto.event.CreateEventRequest;
import br.com.conectabem.dto.event.UpdateEventRequest;
import br.com.conectabem.model.Event;
import br.com.conectabem.model.EventAnnouncement;
import br.com.conectabem.model.EventRegistration;
import br.com.conectabem.model.ParticipationStatus;
import br.com.conectabem.model.User;
import br.com.conectabem.model.UserRole;
import br.com.conectabem.repository.EventAnnouncementRepository;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceUnitTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventRegistrationRepository registrationRepository;

    @Mock
    private EventAnnouncementRepository announcementRepository;

    @Mock
    private UserRepository userRepository;

    private EventServiceImpl eventService;

    @BeforeEach
    void setup() {
        eventService = new EventServiceImpl(eventRepository, registrationRepository, announcementRepository, userRepository);
    }

    @Test
    void createSavesEventWithOwnerAndActivityType() {
        UUID ownerId = UUID.randomUUID();
        CreateEventRequest request = new CreateEventRequest();
        request.setTitle("Community Meetup");
        request.setDescription("Tech and jobs");
        request.setLocation("Sao Paulo");
        request.setActivityType("Workshop");
        request.setStartsAt(Instant.parse("2026-03-10T10:00:00Z"));
        request.setEndsAt(Instant.parse("2026-03-10T12:00:00Z"));
        request.setCapacity(20);

        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event created = eventService.create(request, ownerId);

        assertNotNull(created.getId());
        assertEquals(ownerId, created.getOwnerId());
        assertEquals("Workshop", created.getActivityType());
        assertEquals(20, created.getCapacity());
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

        when(eventRepository.findAllByOrderByStartsAtAsc()).thenReturn(List.of(matching, wrongLocation));

        List<Event> filtered = eventService.findAvailable("sao paulo", "limpeza", "2026-03-19");

        assertEquals(1, filtered.size());
        assertEquals(matching.getId(), filtered.getFirst().getId());
    }

    @Test
    void updateRejectsCapacityBelowActiveRegistrations() {
        UUID ownerId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Event event = Event.builder()
                .id(eventId)
                .ownerId(ownerId)
                .title("Old")
                .location("Sao Paulo")
                .activityType("Mutirao")
                .startsAt(Instant.now().plusSeconds(3600))
                .capacity(10)
                .createdAt(Instant.now())
                .build();
        UpdateEventRequest request = new UpdateEventRequest();
        request.setTitle("New");
        request.setDescription("Desc");
        request.setLocation("Sao Paulo");
        request.setActivityType("Workshop");
        request.setStartsAt(Instant.now().plusSeconds(7200));
        request.setEndsAt(Instant.now().plusSeconds(10800));
        request.setCapacity(1);

        when(eventRepository.findByIdAndOwnerId(eventId, ownerId)).thenReturn(Optional.of(event));
        when(registrationRepository.countByEventIdAndStatusIn(eq(eventId), any(Set.class))).thenReturn(2L);

        assertThrows(IllegalArgumentException.class, () -> eventService.update(eventId, request, ownerId));
    }

    @Test
    void createAnnouncementRequiresOwnerAndPersistsMessage() {
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
        CreateAnnouncementRequest request = new CreateAnnouncementRequest();
        request.setMessage("Levem documento com foto.");

        when(eventRepository.findByIdAndOwnerId(eventId, ownerId)).thenReturn(Optional.of(event));
        when(announcementRepository.save(any(EventAnnouncement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var created = eventService.createAnnouncement(eventId, request, ownerId);

        assertEquals(eventId, created.getEventId());
        assertEquals("Levem documento com foto.", created.getMessage());
    }

    @Test
    void deleteAllowsAdminToRemoveEvent() {
        UUID adminId = UUID.randomUUID();
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
        User admin = User.builder()
                .id(adminId)
                .email("admin@example.com")
                .fullName("Admin")
                .password("HASH")
                .role(UserRole.ADMIN.name())
                .createdAt(Instant.now())
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        boolean deleted = eventService.delete(eventId, adminId);

        assertTrue(deleted);
        verify(eventRepository).delete(event);
    }

    @Test
    void listAnnouncementsAllowsRegisteredVolunteer() {
        UUID eventId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        Event event = Event.builder()
                .id(eventId)
                .ownerId(UUID.randomUUID())
                .title("A")
                .location("Sao Paulo")
                .activityType("Mutirao")
                .startsAt(Instant.now())
                .createdAt(Instant.now())
                .build();
        EventRegistration registration = EventRegistration.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .volunteerId(volunteerId)
                .status(ParticipationStatus.REGISTERED)
                .registeredAt(Instant.now())
                .build();
        EventAnnouncement announcement = EventAnnouncement.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .authorId(event.getOwnerId())
                .message("Aviso")
                .createdAt(Instant.now())
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(registrationRepository.findByEventIdAndVolunteerId(eventId, volunteerId)).thenReturn(Optional.of(registration));
        when(announcementRepository.findAllByEventIdOrderByCreatedAtDesc(eventId)).thenReturn(List.of(announcement));

        var announcements = eventService.listAnnouncements(eventId, volunteerId);

        assertEquals(1, announcements.size());
        assertEquals("Aviso", announcements.getFirst().getMessage());
    }

    @Test
    void buildReportAggregatesCountsByStatus() {
        UUID ownerId = UUID.randomUUID();
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

        var report = eventService.buildReport(eventId, ownerId);

        assertEquals(3, report.getTotalRegistrations());
        assertEquals(7L, report.getAvailableSpots());
        assertEquals(1L, report.getCountsByStatus().get("PRESENT"));
    }
}
