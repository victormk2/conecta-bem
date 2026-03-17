package br.com.conectabem.service;

import br.com.conectabem.dto.event.CreateEventRequest;
import br.com.conectabem.dto.event.EventDTO;
import br.com.conectabem.dto.event.EventReportDTO;
import br.com.conectabem.dto.event.UpdateEventRequest;
import br.com.conectabem.model.Event;
import br.com.conectabem.model.EventRegistration;
import br.com.conectabem.model.ParticipationStatus;
import br.com.conectabem.model.UserRole;
import br.com.conectabem.repository.EventRegistrationRepository;
import br.com.conectabem.repository.EventRepository;
import br.com.conectabem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

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
    public Event create(CreateEventRequest request) {
        validateDateRange(request.getStartsAt(), request.getEndsAt());

        UUID ownerId = currentUserService.requireUserId();

        Event event = Event.builder()
                .id(UUID.randomUUID())
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .activityType(request.getActivityType())
                .startsAt(request.getStartsAt())
                .endsAt(request.getEndsAt())
                .capacity(request.getCapacity())
                .ownerId(ownerId)
                .createdAt(Instant.now())
                .build();

        return eventRepository.save(event);
    }

    @Override
    public List<Event> listMine() {
        return eventRepository.findAllByOwnerIdOrderByStartsAtAsc(currentUserService.requireUserId());
    }

    @Override
    public List<Event> findAvailable(String location, String activityType, String fromDate) {
        LocalDate from = parseDate(fromDate);
        return eventRepository.findAllByOrderByStartsAtAsc()
                .stream()
                .filter(event -> location == null || containsIgnoreCase(event.getLocation(), location))
                .filter(event -> activityType == null || containsIgnoreCase(event.getActivityType(), activityType))
                .filter(event -> from == null || !event.getStartsAt().atZone(ZoneId.systemDefault()).toLocalDate().isBefore(from))
                .toList();
    }

    @Override
    public Optional<Event> findOwnedById(UUID eventId) {
        return eventRepository.findByIdAndOwnerId(eventId, currentUserService.requireUserId());
    }

    @Override
    public Optional<Event> findAccessibleById(UUID eventId) {
        UUID userId = currentUserService.requireUserId();
        return eventRepository.findById(eventId)
                .filter(event -> canAccessEvent(event, userId));
    }

    @Override
    public Optional<Event> update(UUID eventId, UpdateEventRequest request) {
        return eventRepository.findByIdAndOwnerId(eventId, currentUserService.requireUserId())
                .map(event -> {
                    validateDateRange(request.getStartsAt(), request.getEndsAt());
                    validateCapacityChange(eventId, request.getCapacity());

                    event.setTitle(request.getTitle());
                    event.setDescription(request.getDescription());
                    event.setLocation(request.getLocation());
                    event.setActivityType(request.getActivityType());
                    event.setStartsAt(request.getStartsAt());
                    event.setEndsAt(request.getEndsAt());
                    event.setCapacity(request.getCapacity());
                    event.setUpdatedAt(Instant.now());
                    return eventRepository.save(event);
                });
    }

    @Override
    public boolean delete(UUID eventId) {
        UUID requesterId = currentUserService.requireUserId();
        Optional<Event> event = eventRepository.findById(eventId)
                .filter(found -> found.getOwnerId().equals(requesterId) || isAdmin(requesterId));
        event.ifPresent(eventRepository::delete);
        return event.isPresent();
    }

    @Override
    public EventReportDTO buildReport(UUID eventId) {
        Event event = getOwnedEvent(eventId, currentUserService.requireUserId());
        List<EventRegistration> registrations = registrationRepository.findAllByEventIdOrderByRegisteredAtAsc(eventId);

        Map<String, Long> counts = new LinkedHashMap<>();
        for (ParticipationStatus status : ParticipationStatus.values()) {
            counts.put(status.name(), registrations.stream().filter(r -> r.getStatus() == status).count());
        }

        EventReportDTO report = new EventReportDTO();
        report.setEvent(toEventDTO(event));
        report.setTotalRegistrations(registrations.size());
        report.setAvailableSpots(calculateAvailableSpots(event));
        report.setCountsByStatus(counts);
        return report;
    }

    @Override
    public long getAvailableSpots(UUID eventId) {
        return calculateAvailableSpots(loadEvent(eventId));
    }

    private void validateCapacityChange(UUID eventId, Integer capacity) {
        if (capacity == null) {
            return;
        }

        long occupied = registrationRepository.countByEventIdAndStatusIn(eventId, OCCUPYING_STATUSES);
        if (capacity < occupied) {
            throw new IllegalArgumentException("capacity cannot be lower than active registrations");
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

    private boolean canAccessEvent(Event event, UUID userId) {
        return true;
    }
    
    private boolean isAdmin(UUID requesterId) {
        return userRepository.findById(requesterId)
                .map(u -> u.getRole())
                .filter(role -> role == UserRole.ADMIN)
                .isPresent();
    }

    private void validateDateRange(Instant startsAt, Instant endsAt) {
        if (endsAt != null && startsAt.isAfter(endsAt)) {
            throw new IllegalArgumentException("startsAt must be before or equal to endsAt");
        }
    }

    private boolean containsIgnoreCase(String source, String filter) {
        return source != null && source.toLowerCase().contains(filter.toLowerCase());
    }

    private LocalDate parseDate(String fromDate) {
        if (fromDate == null || fromDate.isBlank()) {
            return null;
        }
        return LocalDate.parse(fromDate);
    }

    private EventDTO toEventDTO(Event event) {
        EventDTO dto = new EventDTO();
        dto.setId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setDescription(event.getDescription());
        dto.setLocation(event.getLocation());
        dto.setActivityType(event.getActivityType());
        dto.setStartsAt(event.getStartsAt());
        dto.setEndsAt(event.getEndsAt());
        dto.setCapacity(event.getCapacity());
        dto.setAvailableSpots(calculateAvailableSpots(event));
        dto.setOwnerId(event.getOwnerId());
        dto.setCreatedAt(event.getCreatedAt());
        dto.setUpdatedAt(event.getUpdatedAt());
        return dto;
    }

    private long calculateAvailableSpots(Event event) {
        if (event.getCapacity() == null) {
            return -1;
        }
        long occupied = registrationRepository.countByEventIdAndStatusIn(event.getId(), OCCUPYING_STATUSES);
        return Math.max(event.getCapacity() - occupied, 0);
    }
}
