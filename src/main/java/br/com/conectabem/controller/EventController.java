package br.com.conectabem.controller;

import br.com.conectabem.dto.event.CreateAnnouncementRequest;
import br.com.conectabem.dto.event.CreateEventRequest;
import br.com.conectabem.dto.event.EventAnnouncementDTO;
import br.com.conectabem.dto.event.EventDTO;
import br.com.conectabem.dto.event.EventReportDTO;
import br.com.conectabem.dto.event.UpdateEventRequest;
import br.com.conectabem.model.Event;
import br.com.conectabem.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService service;

    public EventController(EventService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<EventDTO> create(@Valid @RequestBody CreateEventRequest request,
                                           Authentication authentication) {
        UUID userId = requireUserId(authentication);
        Event created = service.create(request, userId);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(toDTO(created));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<EventDTO>> listMine(Authentication authentication) {
        UUID userId = requireUserId(authentication);
        List<EventDTO> events = service.findAllByOwner(userId).stream().map(this::toDTO).toList();
        return ResponseEntity.ok(events);
    }

    @GetMapping
    public ResponseEntity<List<EventDTO>> listAvailable(@RequestParam(required = false) String location,
                                                        @RequestParam(required = false) String activityType,
                                                        @RequestParam(required = false) String fromDate,
                                                        Authentication authentication) {
        requireUserId(authentication);
        List<EventDTO> events = service.findAvailable(location, activityType, fromDate)
                .stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventDTO> getById(@PathVariable UUID id, Authentication authentication) {
        UUID userId = requireUserId(authentication);
        return service.findAccessibleById(id, userId)
                .map(event -> ResponseEntity.ok(toDTO(event)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventDTO> update(@PathVariable UUID id,
                                           @Valid @RequestBody UpdateEventRequest request,
                                           Authentication authentication) {
        UUID userId = requireUserId(authentication);
        return service.update(id, request, userId)
                .map(event -> ResponseEntity.ok(toDTO(event)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication authentication) {
        UUID userId = requireUserId(authentication);
        return service.delete(id, userId)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/announcements")
    public ResponseEntity<EventAnnouncementDTO> createAnnouncement(@PathVariable UUID id,
                                                                   @Valid @RequestBody CreateAnnouncementRequest request,
                                                                   Authentication authentication) {
        UUID userId = requireUserId(authentication);
        EventAnnouncementDTO announcement = service.createAnnouncement(id, request, userId);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{announcementId}")
                .buildAndExpand(announcement.getId())
                .toUri();
        return ResponseEntity.created(location).body(announcement);
    }

    @GetMapping("/{id}/announcements")
    public ResponseEntity<List<EventAnnouncementDTO>> listAnnouncements(@PathVariable UUID id,
                                                                        Authentication authentication) {
        UUID userId = requireUserId(authentication);
        return ResponseEntity.ok(service.listAnnouncements(id, userId));
    }

    @GetMapping("/{id}/report")
    public ResponseEntity<EventReportDTO> report(@PathVariable UUID id, Authentication authentication) {
        UUID userId = requireUserId(authentication);
        return ResponseEntity.ok(service.buildReport(id, userId));
    }

    private UUID requireUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("authentication required");
        }
        return (UUID) authentication.getPrincipal();
    }

    private EventDTO toDTO(Event event) {
        EventDTO dto = new EventDTO();
        dto.setId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setDescription(event.getDescription());
        dto.setLocation(event.getLocation());
        dto.setActivityType(event.getActivityType());
        dto.setStartsAt(event.getStartsAt());
        dto.setEndsAt(event.getEndsAt());
        dto.setCapacity(event.getCapacity());
        dto.setAvailableSpots(service.getAvailableSpots(event.getId()));
        dto.setOwnerId(event.getOwnerId());
        dto.setCreatedAt(event.getCreatedAt());
        dto.setUpdatedAt(event.getUpdatedAt());
        return dto;
    }
}
