package br.com.conectabem.service.impl;

import br.com.conectabem.dto.event.EventCreationDTO;
import br.com.conectabem.dto.event.EventListRequest;
import br.com.conectabem.dto.event.EventListResponse;
import br.com.conectabem.dto.event.EventResponse;
import br.com.conectabem.dto.event.EventUpdateDTO;
import br.com.conectabem.infra.util.Mapper;
import br.com.conectabem.model.Event;
import br.com.conectabem.model.EventCategory;
import br.com.conectabem.model.EventType;
import br.com.conectabem.model.ParticipationStatus;
import br.com.conectabem.model.User;
import br.com.conectabem.repository.EventRegistrationRepository;
import br.com.conectabem.repository.EventRepository;
import br.com.conectabem.service.AddressService;
import br.com.conectabem.service.CurrentUserService;
import br.com.conectabem.service.EventService;
import br.com.conectabem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private static final Set<ParticipationStatus> ACTIVE_STATUSES =
            Set.of(
                    ParticipationStatus.REGISTERED,
                    ParticipationStatus.PENDING,
                    ParticipationStatus.CONFIRMED,
                    ParticipationStatus.PRESENT
            );

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final Mapper<EventCreationDTO, Event> creationToEntity;
    private final UserService userService;
    private final AddressService addressService;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public Event create(EventCreationDTO eventCreationDTO) {
        return createWithImage(eventCreationDTO, null);
    }

    @Override
    @Transactional
    public Event createWithImage(EventCreationDTO eventCreationDTO, MultipartFile image) {
        var baseEntity = creationToEntity.map(eventCreationDTO);
        var owner = userService.findById(currentUserService.requireUserId());
        validateOwnerProfileForEventCreation(owner);
        validateEventForSave(baseEntity, eventCreationDTO.addressId());
        validateEventTypeRequirements(baseEntity);
        baseEntity.setOwner(owner);
        baseEntity.setAddress(addressService.findById(parseUuid(eventCreationDTO.addressId(), "Select a valid address for the event.")));
        applyImageIfPresent(baseEntity, image);
        return eventRepository.save(baseEntity);
    }

    @Override
    @Transactional
    public Event update(EventUpdateDTO eventUpdateDTO) {
        return updateWithImage(eventUpdateDTO, null);
    }

    @Override
    @Transactional
    public Event updateWithImage(EventUpdateDTO eventUpdateDTO, MultipartFile image) {
        var eventOptional = eventRepository.findById(UUID.fromString(eventUpdateDTO.id()));
        if (eventOptional.isPresent()) {
            var event = eventOptional.get();
            event.setTitle(eventUpdateDTO.title());
            event.setDescription(eventUpdateDTO.description());
            event.setCategory(EventCategory.valueOf(eventUpdateDTO.category()));
            event.setStartsAt(LocalDateTime.parse(eventUpdateDTO.startsAt()));
            event.setEndsAt(LocalDateTime.parse(eventUpdateDTO.endsAt()));
            event.setUpdatedAt(LocalDateTime.now());
            event.setCapacity(eventUpdateDTO.capacity());
            applyEventTypeUpdate(event, eventUpdateDTO);
            validateEventForSave(event, eventUpdateDTO.addressId());
            validateEventTypeRequirements(event);

            var addressId = parseUuid(eventUpdateDTO.addressId(), "Select a valid address for the event.");
            if (!event.getAddress().getId().equals(addressId)) {
                event.setAddress(addressService.findById(addressId));
            }

            applyImageIfPresent(event, image);
            return eventRepository.save(event);
        }
        return null;
    }

    @Override
    @Transactional
    public boolean removeImageByEventId(String eventId) {
        var eventOptional = eventRepository.findById(UUID.fromString(eventId));
        if (eventOptional.isEmpty()) return false;

        var event = eventOptional.get();
        if (event.getImage() == null) return true;

        event.setImage(null);
        eventRepository.save(event);
        return true;
    }

    @Override
    public Event findById(String eventId) {
        return eventRepository.findById(UUID.fromString(eventId)).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public EventListResponse list(EventListRequest eventListRequest) {
        LocalDateTime now = LocalDateTime.now();

        List<EventResponse> events = eventRepository.findAll()
                .stream()
                .filter(e -> e.getEndsAt() == null || !e.getEndsAt().isBefore(now))
                .map(this::toEventResponse)
                .toList();

        return new EventListResponse(events, (long) events.size());
    }

    @Override
    public EventResponse toEventResponse(Event event) {
        long activeCount = registrationRepository
                .countByEventIdAndStatusIn(event.getId(), ACTIVE_STATUSES);

        String imageUrl = null;
        if (event.getImage() != null) {
            imageUrl = "data:image/*;base64," +
                    Base64.getEncoder().encodeToString(event.getImage());
        }

        return new EventResponse(
                event.getId(),
                event.getOwner().getId(),
                event.getTitle(),
                event.getDescription(),
                event.getAddress(),
                event.getCategory(),
                event.getStartsAt(),
                event.getEndsAt(),
                event.getCapacity(),
                activeCount,
                imageUrl,
                event.getType(),
                event.getOrganizationName(),
                event.getOrganizationDocument()
        );
    }

    private void applyImageIfPresent(Event event, MultipartFile image) {
        if (image == null || image.isEmpty()) return;

        if (image.getContentType() == null || !image.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("Uploaded file must be an image.");
        }

        try {
            event.setImage(image.getBytes());
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not process uploaded image.");
        }
    }

    private void validateOwnerProfileForEventCreation(User owner) {
        if (owner == null) {
            throw new IllegalArgumentException("Authenticated user not found.");
        }

        if (isBlank(owner.getFullName()) || isBlank(owner.getEmail()) || isBlank(owner.getCpfCnpj()) || isBlank(owner.getPhone())) {
            throw new IllegalArgumentException("Complete your profile before creating an event. Required fields: fullName, email, cpfCnpj, phone.");
        }
    }

    private void validateEventTypeRequirements(Event event) {
        if (event.getType() == null) {
            event.setType(EventType.COMMUNITY);
        }

        if (event.getType() == EventType.ORGANIZATION &&
                (isBlank(event.getOrganizationName()) || isBlank(event.getOrganizationDocument()))) {
            throw new IllegalArgumentException("Organization events require organizationName and organizationDocument.");
        }

        if (event.getType() == EventType.ORGANIZATION && !event.getOrganizationDocument().matches("\\d{14}")) {
            throw new IllegalArgumentException("Organization document must have 14 digits.");
        }

        if (event.getType() == EventType.COMMUNITY) {
            event.setOrganizationName(null);
            event.setOrganizationDocument(null);
        }
    }

    private EventType parseEventType(String value) {
        if (value == null || value.isBlank()) {
            return EventType.COMMUNITY;
        }
        return EventType.valueOf(value);
    }

    private void applyEventTypeUpdate(Event event, EventUpdateDTO eventUpdateDTO) {
        if (eventUpdateDTO.type() != null && !eventUpdateDTO.type().isBlank()) {
            event.setType(parseEventType(eventUpdateDTO.type()));
        }
        if (eventUpdateDTO.organizationName() != null) {
            event.setOrganizationName(trimToNull(eventUpdateDTO.organizationName()));
        }
        if (eventUpdateDTO.organizationDocument() != null) {
            event.setOrganizationDocument(trimToNull(eventUpdateDTO.organizationDocument()));
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void validateEventForSave(Event event, String addressId) {
        if (event == null) {
            throw new IllegalArgumentException("Event data is required.");
        }
        if (isBlank(event.getTitle())) {
            throw new IllegalArgumentException("Event title is required.");
        }
        if (isBlank(event.getDescription())) {
            throw new IllegalArgumentException("Event description is required.");
        }
        if (event.getCategory() == null) {
            throw new IllegalArgumentException("Event category is required.");
        }
        if (event.getStartsAt() == null) {
            throw new IllegalArgumentException("Event start date is required.");
        }
        if (event.getEndsAt() == null) {
            throw new IllegalArgumentException("Event end date is required.");
        }
        if (!event.getEndsAt().isAfter(event.getStartsAt())) {
            throw new IllegalArgumentException("Event end date must be after start date.");
        }
        if (event.getStartsAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Event start date must be in the future.");
        }
        if (event.getCapacity() == null || event.getCapacity() < 1) {
            throw new IllegalArgumentException("Event capacity must be greater than zero.");
        }
        parseUuid(addressId, "Select a valid address for the event.");
    }

    private UUID parseUuid(String value, String errorMessage) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(errorMessage);
        }

        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
