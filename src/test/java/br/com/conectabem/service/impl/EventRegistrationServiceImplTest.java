package br.com.conectabem.service.impl;

import br.com.conectabem.dto.eventregistration.EventRegistrationDecisionRequest;
import br.com.conectabem.model.Event;
import br.com.conectabem.model.EventRegistration;
import br.com.conectabem.model.ParticipationStatus;
import br.com.conectabem.model.User;
import br.com.conectabem.repository.EventRegistrationRepository;
import br.com.conectabem.repository.EventRepository;
import br.com.conectabem.service.CurrentUserService;
import br.com.conectabem.service.UserService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventRegistrationServiceImplTest {

    @Mock
    private EventRegistrationRepository registrationRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private UserService userService;

    @InjectMocks
    private EventRegistrationServiceImpl registrationService;

    @Nested
    class EnrollTest {

        @Test
        void shouldCreatePendingRegistrationSuccessfully() {
            var userId = UUID.randomUUID();
            var ownerId = UUID.randomUUID();
            var now = LocalDateTime.now();
            var event = buildEvent(ownerId, now.plusDays(1), now.plusDays(1).plusHours(3), 10);
            var user = user(userId);

            when(currentUserService.requireUserId()).thenReturn(userId);
            when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
            when(userService.findById(userId)).thenReturn(user);
            when(registrationRepository.findByEventIdAndVolunteerId(event.getId(), userId))
                    .thenReturn(Optional.empty());
            when(registrationRepository.countByEventIdAndStatusIn(eq(event.getId()), anyCollection()))
                    .thenReturn(0L);

            registrationService.enroll(event.getId());

            var captor = ArgumentCaptor.forClass(EventRegistration.class);
            verify(registrationRepository).save(captor.capture());

            var saved = captor.getValue();
            assertThat(saved.getEvent()).isEqualTo(event);
            assertThat(saved.getVolunteer()).isEqualTo(user);
            assertThat(saved.getStatus()).isEqualTo(ParticipationStatus.PENDING);
            assertThat(saved.getStatusUpdatedAt()).isNotNull();
        }

        @Test
        void shouldThrowWhenOwnerTriesToEnrollInOwnEvent() {
            var ownerId = UUID.randomUUID();
            var now = LocalDateTime.now();
            var event = buildEvent(ownerId, now.plusDays(1), now.plusDays(1).plusHours(3), 10);

            when(currentUserService.requireUserId()).thenReturn(ownerId);
            when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
            when(userService.findById(ownerId)).thenReturn(user(ownerId));

            assertThatThrownBy(() -> registrationService.enroll(event.getId()))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("dono do evento");

            verify(registrationRepository, never()).save(any());
        }

        @Test
        void shouldThrowWhenCapacityIsFull() {
            var userId = UUID.randomUUID();
            var ownerId = UUID.randomUUID();
            var now = LocalDateTime.now();
            var event = buildEvent(ownerId, now.plusDays(1), now.plusDays(1).plusHours(3), 2);

            when(currentUserService.requireUserId()).thenReturn(userId);
            when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
            when(userService.findById(userId)).thenReturn(user(userId));
            when(registrationRepository.findByEventIdAndVolunteerId(event.getId(), userId))
                    .thenReturn(Optional.empty());
            when(registrationRepository.countByEventIdAndStatusIn(eq(event.getId()), anyCollection()))
                    .thenReturn(2L);

            assertThatThrownBy(() -> registrationService.enroll(event.getId()))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("número máximo");

            verify(registrationRepository, never()).save(any());
        }

        @Test
        void shouldThrowWhenUserIsAlreadyActivelyEnrolled() {
            var userId = UUID.randomUUID();
            var ownerId = UUID.randomUUID();
            var now = LocalDateTime.now();
            var event = buildEvent(ownerId, now.plusDays(1), now.plusDays(1).plusHours(3), 10);

            var existingRegistration = EventRegistration.builder()
                    .id(UUID.randomUUID())
                    .event(event)
                    .status(ParticipationStatus.PENDING)
                    .build();

            when(currentUserService.requireUserId()).thenReturn(userId);
            when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
            when(userService.findById(userId)).thenReturn(user(userId));
            when(registrationRepository.findByEventIdAndVolunteerId(event.getId(), userId))
                    .thenReturn(Optional.of(existingRegistration));

            assertThatThrownBy(() -> registrationService.enroll(event.getId()))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("já está inscrito");

            verify(registrationRepository, never()).save(any());
        }
    }

    @Nested
    class CancelTest {

        @Test
        void shouldCancelSuccessfullyBeforeEventStarts() {
            var userId = UUID.randomUUID();
            var ownerId = UUID.randomUUID();
            var now = LocalDateTime.now();
            var event = buildEvent(ownerId, now.plusDays(1), now.plusDays(1).plusHours(3), 10);

            var registration = EventRegistration.builder()
                    .id(UUID.randomUUID())
                    .event(event)
                    .status(ParticipationStatus.PENDING)
                    .build();

            when(currentUserService.requireUserId()).thenReturn(userId);
            when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
            when(registrationRepository.findByEventIdAndVolunteerId(event.getId(), userId))
                    .thenReturn(Optional.of(registration));

            registrationService.cancel(event.getId());

            assertThat(registration.getStatus()).isEqualTo(ParticipationStatus.CANCELLED);
            assertThat(registration.getStatusUpdatedAt()).isNotNull();
            verify(registrationRepository).save(registration);
        }
    }

    @Nested
    class EnrollmentStatusTest {

        @Test
        void shouldReturnTrueWhenUserHasPendingRegistration() {
            var userId = UUID.randomUUID();
            var eventId = UUID.randomUUID();

            var registration = EventRegistration.builder()
                    .id(UUID.randomUUID())
                    .status(ParticipationStatus.PENDING)
                    .build();

            when(currentUserService.requireUserId()).thenReturn(userId);
            when(registrationRepository.findByEventIdAndVolunteerId(eventId, userId))
                    .thenReturn(Optional.of(registration));

            var result = registrationService.getEnrollmentStatus(eventId);

            assertThat(result.enrolled()).isTrue();
        }

        @Test
        void shouldReturnFalseWhenRegistrationIsCancelled() {
            var userId = UUID.randomUUID();
            var eventId = UUID.randomUUID();

            var registration = EventRegistration.builder()
                    .id(UUID.randomUUID())
                    .status(ParticipationStatus.CANCELLED)
                    .build();

            when(currentUserService.requireUserId()).thenReturn(userId);
            when(registrationRepository.findByEventIdAndVolunteerId(eventId, userId))
                    .thenReturn(Optional.of(registration));

            var result = registrationService.getEnrollmentStatus(eventId);

            assertThat(result.enrolled()).isFalse();
        }
    }

    @Nested
    class DecisionTest {
        @Test
        void shouldRejectPendingRegistrationWhenCurrentUserOwnsEvent() {
            var ownerId = UUID.randomUUID();
            var registration = registration(ParticipationStatus.PENDING, ownerId);
            var request = new EventRegistrationDecisionRequest("Não atende ao perfil da ação");

            when(currentUserService.requireUserId()).thenReturn(ownerId);
            when(registrationRepository.findById(registration.getId())).thenReturn(Optional.of(registration));
            when(registrationRepository.save(registration)).thenReturn(registration);

            var result = registrationService.reject(registration.getId(), request);

            assertThat(result.status()).isEqualTo("REJECTED");
            assertThat(result.justification()).isEqualTo("Não atende ao perfil da ação");
            assertThat(registration.getStatusUpdatedAt()).isNotNull();
        }

        @Test
        void shouldDismissConfirmedRegistrationWhenCurrentUserOwnsEvent() {
            var ownerId = UUID.randomUUID();
            var registration = registration(ParticipationStatus.CONFIRMED, ownerId);

            when(currentUserService.requireUserId()).thenReturn(ownerId);
            when(registrationRepository.findById(registration.getId())).thenReturn(Optional.of(registration));
            when(registrationRepository.save(registration)).thenReturn(registration);

            var result = registrationService.dismiss(
                    registration.getId(),
                    new EventRegistrationDecisionRequest("Equipe completa")
            );

            assertThat(result.status()).isEqualTo("DISMISSED");
            assertThat(result.justification()).isEqualTo("Equipe completa");
        }

        @Test
        void shouldRejectDecisionWhenCurrentUserDoesNotOwnEvent() {
            var ownerId = UUID.randomUUID();
            var currentUserId = UUID.randomUUID();
            var registration = registration(ParticipationStatus.PENDING, ownerId);

            when(currentUserService.requireUserId()).thenReturn(currentUserId);
            when(registrationRepository.findById(registration.getId())).thenReturn(Optional.of(registration));

            assertThatThrownBy(() -> registrationService.reject(registration.getId(), null))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("dono do evento");

            verify(registrationRepository, never()).save(any(EventRegistration.class));
        }
    }

    @Nested
    class GetParticipantsTest {

        @Test
        void shouldReturnActiveParticipantsWhenCalledByOwner() {
            var ownerId = UUID.randomUUID();
            var now = LocalDateTime.now();
            var event = buildEvent(ownerId, now.plusDays(1), now.plusDays(1).plusHours(3), 10);

            var volunteer = user(UUID.randomUUID());
            volunteer.setFullName("Maria Souza");
            volunteer.setEmail("maria@email.com");
            volunteer.setCpfCnpj("12345678901");
            volunteer.setPhone("47999990000");

            var activeRegistration = EventRegistration.builder()
                    .id(UUID.randomUUID())
                    .event(event)
                    .volunteer(volunteer)
                    .status(ParticipationStatus.PENDING)
                    .build();

            var cancelledRegistration = EventRegistration.builder()
                    .id(UUID.randomUUID())
                    .event(event)
                    .volunteer(new User())
                    .status(ParticipationStatus.CANCELLED)
                    .build();

            when(currentUserService.requireUserId()).thenReturn(ownerId);
            when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
            when(registrationRepository.findAllByEventIdOrderByRegisteredAtAsc(event.getId()))
                    .thenReturn(List.of(activeRegistration, cancelledRegistration));

            var result = registrationService.getParticipants(event.getId());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("Maria Souza");
            assertThat(result.get(0).email()).isEqualTo("maria@email.com");
        }
    }

    @Nested
    class GetMyEnrolledEventsTest {

        @Test
        void shouldReturnOnlyActivelyEnrolledEvents() {
            var userId = UUID.randomUUID();
            var ownerId = UUID.randomUUID();
            var now = LocalDateTime.now();

            var activeEvent = buildEvent(ownerId, now.plusDays(1), now.plusDays(1).plusHours(2), 10);
            var cancelledEvent = buildEvent(ownerId, now.plusDays(2), now.plusDays(2).plusHours(2), 10);

            var activeRegistration = EventRegistration.builder()
                    .id(UUID.randomUUID())
                    .event(activeEvent)
                    .status(ParticipationStatus.PENDING)
                    .build();

            var cancelledRegistration = EventRegistration.builder()
                    .id(UUID.randomUUID())
                    .event(cancelledEvent)
                    .status(ParticipationStatus.CANCELLED)
                    .build();

            when(currentUserService.requireUserId()).thenReturn(userId);
            when(registrationRepository.findAllByVolunteerIdOrderByRegisteredAtDesc(userId))
                    .thenReturn(List.of(activeRegistration, cancelledRegistration));
            when(registrationRepository.countByEventIdAndStatusIn(eq(activeEvent.getId()), anyCollection()))
                    .thenReturn(1L);

            var result = registrationService.getMyEnrolledEvents();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(activeEvent.getId());
        }
    }

    private Event buildEvent(UUID ownerId, LocalDateTime startsAt, LocalDateTime endsAt, Integer capacity) {
        var owner = user(ownerId);

        var event = new Event();
        event.setId(UUID.randomUUID());
        event.setOwner(owner);
        event.setStartsAt(startsAt);
        event.setEndsAt(endsAt);
        event.setCapacity(capacity);
        return event;
    }

    private static EventRegistration registration(ParticipationStatus status, UUID ownerId) {
        var registration = new EventRegistration();
        registration.setId(UUID.randomUUID());
        registration.setEvent(event(UUID.randomUUID(), user(ownerId)));
        registration.setVolunteer(user(UUID.randomUUID()));
        registration.setStatus(status);
        return registration;
    }

    private static Event event(UUID id, User owner) {
        var event = new Event();
        event.setId(id);
        event.setOwner(owner);
        return event;
    }

    private static User user(UUID id) {
        var user = new User();
        user.setId(id);
        return user;
    }
}
