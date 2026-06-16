package br.com.conectabem.service.impl;

import br.com.conectabem.dto.event.EventCreationDTO;
import br.com.conectabem.dto.event.EventListRequest;
import br.com.conectabem.dto.event.EventListResponse;
import br.com.conectabem.dto.event.EventResponse;
import br.com.conectabem.dto.event.EventUpdateDTO;
import br.com.conectabem.infra.util.Mapper;
import br.com.conectabem.model.Event;
import br.com.conectabem.model.EventCategory;
import br.com.conectabem.model.ParticipationStatus;
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
            Set.of(ParticipationStatus.REGISTERED, ParticipationStatus.PRESENT);

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final Mapper<EventCreationDTO, Event> creationToEntity;
    private final UserService userService;
    private final AddressService addressService;
    private final CurrentUserService currentUserService;

    // ── Create ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Event create(EventCreationDTO eventCreationDTO) {
        return createWithImage(eventCreationDTO, null);
    }

    @Override
    @Transactional
    public Event createWithImage(EventCreationDTO eventCreationDTO, MultipartFile image) {
        var baseEntity = creationToEntity.map(eventCreationDTO);
        baseEntity.setOwner(userService.findById(currentUserService.requireUserId()));
        baseEntity.setAddress(addressService.findById(UUID.fromString(eventCreationDTO.addressId())));
        applyImageIfPresent(baseEntity, image);
        return eventRepository.save(baseEntity);
    }

    // ── Update ────────────────────────────────────────────────────────────────

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

            if (!event.getAddress().getId().equals(UUID.fromString(eventUpdateDTO.addressId()))) {
                event.setAddress(addressService.findById(UUID.fromString(eventUpdateDTO.addressId())));
            }

            applyImageIfPresent(event, image);
            return eventRepository.save(event);
        }
        return null;
    }

    // ── Image ─────────────────────────────────────────────────────────────────

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

    // ── Find ──────────────────────────────────────────────────────────────────

    @Override
    public Event findById(String eventId) {
        return eventRepository.findById(UUID.fromString(eventId)).orElse(null);
    }

    // ── List (public — excludes finished events, includes live enrolledCount) ──

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

    // ── DTO conversion ────────────────────────────────────────────────────────

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
                imageUrl
        );
    }

    // ── Internal ──────────────────────────────────────────────────────────────

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
}