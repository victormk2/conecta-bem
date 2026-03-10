package br.com.conectabem.controller;

import br.com.conectabem.dto.event.CreateAnnouncementRequest;
import br.com.conectabem.dto.event.CreateEventRequest;
import br.com.conectabem.dto.event.EventAnnouncementDTO;
import br.com.conectabem.dto.event.EventDTO;
import br.com.conectabem.dto.event.EventReportDTO;
import br.com.conectabem.dto.event.UpdateEventRequest;
import br.com.conectabem.model.Event;
import br.com.conectabem.service.EventService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock
    private EventService eventService;

    private EventController controller;

    private UUID userId;
    private Authentication authentication;

    @BeforeEach
    void setup() {
        controller = new EventController(eventService);
        userId = UUID.randomUUID();
        authentication = new UsernamePasswordAuthenticationToken(userId, null);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("localhost");
        request.setServerPort(8080);
        request.setScheme("http");
        request.setRequestURI("/events");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void createReturnsCreatedEvent() {
        CreateEventRequest request = new CreateEventRequest();
        request.setTitle("Mutirao");
        request.setLocation("Sao Paulo");
        request.setActivityType("Limpeza");
        request.setStartsAt(Instant.parse("2026-03-15T10:00:00Z"));

        Event created = sampleEvent();
        when(eventService.create(request, userId)).thenReturn(created);
        when(eventService.getAvailableSpots(created.getId())).thenReturn(10L);

        ResponseEntity<EventDTO> response = controller.create(request, authentication);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getHeaders().getLocation());
        assertEquals(created.getId(), response.getBody().getId());
        assertEquals(10L, response.getBody().getAvailableSpots());
    }

    @Test
    void listMineReturnsOwnedEvents() {
        Event event = sampleEvent();
        when(eventService.findAllByOwner(userId)).thenReturn(List.of(event));
        when(eventService.getAvailableSpots(event.getId())).thenReturn(4L);

        ResponseEntity<List<EventDTO>> response = controller.listMine(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(4L, response.getBody().getFirst().getAvailableSpots());
    }

    @Test
    void listAvailableReturnsFilteredEvents() {
        Event event = sampleEvent();
        when(eventService.findAvailable("Sao Paulo", "Limpeza", "2026-03-10")).thenReturn(List.of(event));
        when(eventService.getAvailableSpots(event.getId())).thenReturn(3L);

        ResponseEntity<List<EventDTO>> response =
                controller.listAvailable("Sao Paulo", "Limpeza", "2026-03-10", authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(event.getId(), response.getBody().getFirst().getId());
    }

    @Test
    void getByIdReturnsNotFoundWhenServiceDoesNotFindEvent() {
        UUID eventId = UUID.randomUUID();
        when(eventService.findAccessibleById(eventId, userId)).thenReturn(Optional.empty());

        ResponseEntity<EventDTO> response = controller.getById(eventId, authentication);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void updateReturnsUpdatedEvent() {
        UUID eventId = UUID.randomUUID();
        UpdateEventRequest request = new UpdateEventRequest();
        request.setTitle("Atualizado");
        request.setLocation("Sao Paulo");
        request.setActivityType("Doacao");
        request.setStartsAt(Instant.parse("2026-03-20T10:00:00Z"));
        Event event = sampleEvent();
        when(eventService.update(eventId, request, userId)).thenReturn(Optional.of(event));
        when(eventService.getAvailableSpots(event.getId())).thenReturn(6L);

        ResponseEntity<EventDTO> response = controller.update(eventId, request, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(6L, response.getBody().getAvailableSpots());
    }

    @Test
    void deleteReturnsNoContentWhenDeleted() {
        UUID eventId = UUID.randomUUID();
        when(eventService.delete(eventId, userId)).thenReturn(true);

        ResponseEntity<Void> response = controller.delete(eventId, authentication);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void createAnnouncementReturnsCreatedMessage() {
        UUID eventId = UUID.randomUUID();
        CreateAnnouncementRequest request = new CreateAnnouncementRequest();
        request.setMessage("Aviso");
        EventAnnouncementDTO dto = new EventAnnouncementDTO();
        dto.setId(UUID.randomUUID());
        dto.setEventId(eventId);
        when(eventService.createAnnouncement(eventId, request, userId)).thenReturn(dto);

        ResponseEntity<EventAnnouncementDTO> response = controller.createAnnouncement(eventId, request, authentication);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getHeaders().getLocation().toString().contains(dto.getId().toString()));
    }

    @Test
    void listAnnouncementsReturnsMessages() {
        UUID eventId = UUID.randomUUID();
        EventAnnouncementDTO dto = new EventAnnouncementDTO();
        dto.setEventId(eventId);
        when(eventService.listAnnouncements(eventId, userId)).thenReturn(List.of(dto));

        ResponseEntity<List<EventAnnouncementDTO>> response = controller.listAnnouncements(eventId, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void reportReturnsEventReport() {
        UUID eventId = UUID.randomUUID();
        EventReportDTO report = new EventReportDTO();
        report.setTotalRegistrations(5);
        when(eventService.buildReport(eventId, userId)).thenReturn(report);

        ResponseEntity<EventReportDTO> response = controller.report(eventId, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(5, response.getBody().getTotalRegistrations());
    }

    @Test
    void methodsThatRequireAuthThrowWhenAuthenticationMissing() {
        assertThrows(IllegalArgumentException.class, () -> controller.listMine(null));
        assertThrows(IllegalArgumentException.class, () -> controller.listAvailable(null, null, null, null));
    }

    private Event sampleEvent() {
        return Event.builder()
                .id(UUID.randomUUID())
                .title("Mutirao Solidario")
                .description("Descricao")
                .location("Sao Paulo")
                .activityType("Limpeza")
                .startsAt(Instant.parse("2026-03-15T10:00:00Z"))
                .endsAt(Instant.parse("2026-03-15T12:00:00Z"))
                .capacity(10)
                .ownerId(userId)
                .createdAt(Instant.parse("2026-03-09T12:00:00Z"))
                .updatedAt(Instant.parse("2026-03-09T13:00:00Z"))
                .build();
    }
}
