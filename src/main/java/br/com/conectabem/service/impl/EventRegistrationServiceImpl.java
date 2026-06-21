package br.com.conectabem.service.impl;

import br.com.conectabem.dto.event.EnrollmentStatusDTO;
import br.com.conectabem.dto.event.EventResponse;
import br.com.conectabem.dto.event.ParticipantDTO;
import br.com.conectabem.dto.eventregistration.AbsenceNoticeRequest;
import br.com.conectabem.dto.eventregistration.EventRegistrationDecisionRequest;
import br.com.conectabem.dto.eventregistration.EventRegistrationResponse;
import br.com.conectabem.dto.eventregistration.OrganizerFeedbackRequest;
import br.com.conectabem.model.Event;
import br.com.conectabem.model.EventRegistration;
import br.com.conectabem.model.ParticipationStatus;
import br.com.conectabem.model.User;
import br.com.conectabem.repository.EventRegistrationRepository;
import br.com.conectabem.repository.EventRepository;
import br.com.conectabem.service.CurrentUserService;
import br.com.conectabem.service.EventRegistrationService;
import br.com.conectabem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventRegistrationServiceImpl implements EventRegistrationService {

    private static final Set<ParticipationStatus> ACTIVE_STATUSES = Set.of(
            ParticipationStatus.REGISTERED,
            ParticipationStatus.PENDING,
            ParticipationStatus.CONFIRMED,
            ParticipationStatus.PRESENT
    );

    private static final Set<ParticipationStatus> FEEDBACK_STATUSES = Set.of(
            ParticipationStatus.REGISTERED,
            ParticipationStatus.CONFIRMED,
            ParticipationStatus.PRESENT,
            ParticipationStatus.ABSENT,
            ParticipationStatus.JUSTIFIED
    );

    private final EventRegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final CurrentUserService currentUserService;
    private final UserService userService;

    @Override
    @Transactional
    public void enroll(UUID eventId) {
        UUID userId = currentUserService.requireUserId();
        Event event = requireEvent(eventId);

        if (event.getOwner().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "O dono do evento não pode se inscrever nele.");
        }

        if (event.getEndsAt() != null && LocalDateTime.now().isAfter(event.getEndsAt())) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "Este evento já foi encerrado.");
        }

        var existing = registrationRepository.findByEventIdAndVolunteerId(eventId, userId);

        if (existing.isPresent() && ACTIVE_STATUSES.contains(existing.get().getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Você já está inscrito neste evento.");
        }

        long activeCount = registrationRepository.countByEventIdAndStatusIn(eventId, ACTIVE_STATUSES);
        if (event.getCapacity() != null && activeCount >= event.getCapacity()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "O evento já atingiu o número máximo de voluntários.");
        }

        EventRegistration registration = existing.orElseGet(() -> EventRegistration.builder()
                .event(event)
                .volunteer(requireUser(userId))
                .build());

        registration.setStatus(ParticipationStatus.REGISTERED);
        registration.setStatusUpdatedAt(Instant.now());
        registration.setJustification(null);

        registrationRepository.save(registration);
    }

    @Override
    @Transactional
    public void cancel(UUID eventId) {
        UUID userId = currentUserService.requireUserId();
        Event event = requireEvent(eventId);

        if (LocalDateTime.now().isAfter(event.getStartsAt())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Não é possível cancelar a inscrição após o início do evento.");
        }

        EventRegistration registration = registrationRepository
                .findByEventIdAndVolunteerId(eventId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Inscrição não encontrada."));

        if (!ACTIVE_STATUSES.contains(registration.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Esta inscrição já foi finalizada.");
        }

        registration.setStatus(ParticipationStatus.CANCELED);
        registration.setStatusUpdatedAt(Instant.now());
        registrationRepository.save(registration);
    }

    @Override
    @Transactional
    public EventRegistrationResponse notifyAbsence(UUID eventId, AbsenceNoticeRequest request) {
        UUID userId = currentUserService.requireUserId();
        Event event = requireEvent(eventId);

        if (event.getEndsAt() != null && LocalDateTime.now().isAfter(event.getEndsAt())) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Não é possível avisar ausência após o encerramento do evento.");
        }

        EventRegistration registration = registrationRepository
                .findByEventIdAndVolunteerId(eventId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Inscrição não encontrada."));

        if (!ACTIVE_STATUSES.contains(registration.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Apenas inscrições ativas podem registrar aviso de ausência.");
        }

        String justification = readJustification(request);
        if (justification == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "A justificativa da ausência é obrigatória.");
        }

        registration.setStatus(ParticipationStatus.JUSTIFIED);
        registration.setJustification(justification);
        registration.setStatusUpdatedAt(Instant.now());
        return EventRegistrationResponse.from(registrationRepository.save(registration));
    }

    @Override
    @Transactional(readOnly = true)
    public EnrollmentStatusDTO getEnrollmentStatus(UUID eventId) {
        UUID userId = currentUserService.requireUserId();

        boolean enrolled = registrationRepository
                .findByEventIdAndVolunteerId(eventId, userId)
                .map(r -> ACTIVE_STATUSES.contains(r.getStatus()))
                .orElse(false);

        return new EnrollmentStatusDTO(enrolled);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipantDTO> getParticipants(UUID eventId) {
        UUID userId = currentUserService.requireUserId();
        Event event = requireEvent(eventId);

        if (!event.getOwner().getId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Apenas o dono do evento pode ver os participantes.");
        }

        return registrationRepository
                .findAllByEventIdOrderByRegisteredAtAsc(eventId)
                .stream()
                .filter(r -> ACTIVE_STATUSES.contains(r.getStatus()))
                .map(r -> toParticipantDTO(r.getVolunteer()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getMyEnrolledEvents() {
        UUID userId = currentUserService.requireUserId();

        return registrationRepository
                .findAllByVolunteerIdOrderByRegisteredAtDesc(userId)
                .stream()
                .filter(r -> ACTIVE_STATUSES.contains(r.getStatus()))
                .map(r -> toEventResponse(r.getEvent()))
                .toList();
    }

    @Override
    @Transactional
    public EventRegistrationResponse confirm(UUID registrationId) {
        EventRegistration registration = requireRegistration(registrationId);
        ensureCurrentUserOwnsEvent(registration);
        ensureStatusIn(
                registration,
                Set.of(ParticipationStatus.REGISTERED, ParticipationStatus.PENDING),
                "Apenas inscrições pendentes podem ser confirmadas."
        );

        registration.setStatus(ParticipationStatus.CONFIRMED);
        registration.setStatusUpdatedAt(Instant.now());
        return EventRegistrationResponse.from(registrationRepository.save(registration));
    }

    @Override
    @Transactional
    public EventRegistrationResponse reject(UUID registrationId, EventRegistrationDecisionRequest request) {
        EventRegistration registration = requireRegistration(registrationId);
        ensureCurrentUserOwnsEvent(registration);
        ensureStatusIn(
                registration,
                Set.of(ParticipationStatus.REGISTERED, ParticipationStatus.PENDING),
                "Apenas inscrições pendentes podem ser recusadas."
        );

        registration.setStatus(ParticipationStatus.REJECTED);
        registration.setJustification(readJustification(request));
        registration.setStatusUpdatedAt(Instant.now());
        return EventRegistrationResponse.from(registrationRepository.save(registration));
    }

    @Override
    @Transactional
    public EventRegistrationResponse dismiss(UUID registrationId, EventRegistrationDecisionRequest request) {
        EventRegistration registration = requireRegistration(registrationId);
        ensureCurrentUserOwnsEvent(registration);
        ensureStatusIn(
                registration,
                Set.of(ParticipationStatus.REGISTERED, ParticipationStatus.CONFIRMED),
                "Apenas inscrições confirmadas podem ser dispensadas."
        );

        registration.setStatus(ParticipationStatus.DISMISSED);
        registration.setJustification(readJustification(request));
        registration.setStatusUpdatedAt(Instant.now());
        return EventRegistrationResponse.from(registrationRepository.save(registration));
    }

    @Override
    @Transactional
    public EventRegistrationResponse addOrganizerFeedback(UUID registrationId, OrganizerFeedbackRequest request) {
        EventRegistration registration = requireRegistration(registrationId);
        ensureCurrentUserOwnsEvent(registration);
        ensureEventHasEnded(registration.getEvent());
        ensureFeedbackStatus(registration);

        String comment = readFeedbackComment(request);
        Integer rating = readFeedbackRating(request);
        if (comment == null && rating == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "Informe uma nota ou comentário para registrar o feedback.");
        }

        registration.setFeedbackRating(rating);
        registration.setOrganizerFeedback(comment);
        registration.setFeedbackCreatedAt(Instant.now());
        return EventRegistrationResponse.from(registrationRepository.save(registration));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventRegistrationResponse> listByEvent(UUID eventId) {
        Event event = requireEvent(eventId);
        ensureCurrentUserOwnsEvent(event);

        return registrationRepository.findAllByEventIdOrderByRegisteredAtAsc(eventId)
                .stream()
                .map(EventRegistrationResponse::from)
                .toList();
    }

    private Event requireEvent(UUID eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Evento não encontrado."));
    }

    private EventRegistration requireRegistration(UUID registrationId) {
        return registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Inscrição não encontrada."));
    }

    private User requireUser(UUID userId) {
        User user = userService.findById(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado.");
        }
        return user;
    }

    private void ensureCurrentUserOwnsEvent(EventRegistration registration) {
        ensureCurrentUserOwnsEvent(registration.getEvent());
    }

    private void ensureCurrentUserOwnsEvent(Event event) {
        UUID currentUserId = currentUserService.requireUserId();
        if (!event.getOwner().getId().equals(currentUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Apenas o dono do evento pode gerenciar inscrições.");
        }
    }

    private void ensureStatus(EventRegistration registration, ParticipationStatus expected, String message) {
        if (registration.getStatus() != expected) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, message);
        }
    }

    private void ensureStatusIn(EventRegistration registration, Set<ParticipationStatus> expected, String message) {
        if (!expected.contains(registration.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, message);
        }
    }

    private void ensureEventHasEnded(Event event) {
        if (event.getEndsAt() != null && LocalDateTime.now().isBefore(event.getEndsAt())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Feedback do organizador só pode ser registrado após o evento.");
        }
    }

    private void ensureFeedbackStatus(EventRegistration registration) {
        if (!FEEDBACK_STATUSES.contains(registration.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Esta inscrição ainda não pode receber feedback do organizador.");
        }
    }

    private String readJustification(EventRegistrationDecisionRequest request) {
        if (request == null || request.justification() == null || request.justification().isBlank()) {
            return null;
        }
        return request.justification().trim();
    }

    private String readJustification(AbsenceNoticeRequest request) {
        if (request == null || request.justification() == null || request.justification().isBlank()) {
            return null;
        }
        return request.justification().trim();
    }

    private String readFeedbackComment(OrganizerFeedbackRequest request) {
        if (request == null || request.comment() == null || request.comment().isBlank()) {
            return null;
        }
        return request.comment().trim();
    }

    private Integer readFeedbackRating(OrganizerFeedbackRequest request) {
        if (request == null || request.rating() == null) {
            return null;
        }
        if (request.rating() < 1 || request.rating() > 5) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "A nota do feedback deve estar entre 1 e 5.");
        }
        return request.rating();
    }

    private ParticipantDTO toParticipantDTO(User user) {
        return new ParticipantDTO(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getCpfCnpj(),
                user.getBirthDate(),
                user.getPhone(),
                user.getGender()
        );
    }

    private EventResponse toEventResponse(Event event) {
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
}
