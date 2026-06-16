package br.com.conectabem.service.impl;

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
        void shouldEnrollSuccessfully() {
            var userId = UUID.randomUUID();
            var ownerId = UUID.randomUUID();
            var now = LocalDateTime.now();
            var event = buildEvent(ownerId, now.plusDays(1), now.plusDays(1).plusHours(3), 10);
            var user = new User();
            user.setId(userId);

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
            assertThat(saved.getStatus()).isEqualTo(ParticipationStatus.REGISTERED);
        }

        @Test
        void shouldThrowWhenOwnerTriesToEnrollInOwnEvent() {
            var ownerId = UUID.randomUUID();
            var now = LocalDateTime.now();
            var event = buildEvent(ownerId, now.plusDays(1), now.plusDays(1).plusHours(3), 10);

            when(currentUserService.requireUserId()).thenReturn(ownerId);
            when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

            assertThatThrownBy(() -> registrationService.enroll(event.getId()))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("dono do evento");

            verify(registrationRepository, never()).save(any());
        }

        @Test
        void shouldThrowWhenEventHasAlreadyEnded() {
            var userId = UUID.randomUUID();
            var ownerId = UUID.randomUUID();
            var now = LocalDateTime.now();
            var event = buildEvent(ownerId, now.minusDays(5), now.minusDays(5).plusHours(3), 10);

            when(currentUserService.requireUserId()).thenReturn(userId);
            when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

            assertThatThrownBy(() -> registrationService.enroll(event.getId()))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("encerrado");

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
            when(registrationRepository.findByEventIdAndVolunteerId(event.getId(), userId))
                    .thenReturn(Optional.empty());
            when(registrationRepository.countByEventIdAndStatusIn(eq(event.getId()), anyCollection()))
                    .thenReturn(2L);

            assertThatThrownBy(() -> registrationService.enroll(event.getId()))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("máximo de voluntários");

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
                    .status(ParticipationStatus.REGISTERED)
                    .build();

            when(currentUserService.requireUserId()).thenReturn(userId);
            when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
            when(registrationRepository.findByEventIdAndVolunteerId(event.getId(), userId))
                    .thenReturn(Optional.of(existingRegistration));

            assertThatThrownBy(() -> registrationService.enroll(event.getId()))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("já está inscrito");

            verify(registrationRepository, never()).save(any());
        }

        @Test
        void shouldAllowReEnrollingAfterPreviousCancellation() {
            var userId = UUID.randomUUID();
            var ownerId = UUID.randomUUID();
            var now = LocalDateTime.now();
            var event = buildEvent(ownerId, now.plusDays(1), now.plusDays(1).plusHours(3), 10);
            var user = new User();
            user.setId(userId);

            var canceledRegistration = EventRegistration.builder()
                    .id(UUID.randomUUID())
                    .event(event)
                    .status(ParticipationStatus.CANCELED)
                    .build();

            when(currentUserService.requireUserId()).thenReturn(userId);
            when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
            when(userService.findById(userId)).thenReturn(user);
            when(registrationRepository.findByEventIdAndVolunteerId(event.getId(), userId))
                    .thenReturn(Optional.of(canceledRegistration));
            when(registrationRepository.countByEventIdAndStatusIn(eq(event.getId()), anyCollection()))
                    .thenReturn(0L);

            registrationService.enroll(event.getId());

            verify(registrationRepository).save(any(EventRegistration.class));
        }

        @Test
        void shouldThrowWhenEventDoesNotExist() {
            var userId = UUID.randomUUID();
            var eventId = UUID.randomUUID();

            when(currentUserService.requireUserId()).thenReturn(userId);
            when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> registrationService.enroll(eventId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("não encontrado");

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
                    .status(ParticipationStatus.REGISTERED)
                    .build();

            when(currentUserService.requireUserId()).thenReturn(userId);
            when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
            when(registrationRepository.findByEventIdAndVolunteerId(event.getId(), userId))
                    .thenReturn(Optional.of(registration));

            registrationService.cancel(event.getId());

            assertThat(registration.getStatus()).isEqualTo(ParticipationStatus.CANCELED);
            verify(registrationRepository).save(registration);
        }

        @Test
        void shouldThrowWhenEventHasAlreadyStarted() {
            var userId = UUID.randomUUID();
            var ownerId = UUID.randomUUID();
            var now = LocalDateTime.now();
            var event = buildEvent(ownerId, now.minusHours(1), now.plusHours(3), 10);

            when(currentUserService.requireUserId()).thenReturn(userId);
            when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

            assertThatThrownBy(() -> registrationService.cancel(event.getId()))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("início do evento");

            verify(registrationRepository, never()).save(any());
        }

        @Test
        void shouldThrowWhenRegistrationDoesNotExist() {
            var userId = UUID.randomUUID();
            var ownerId = UUID.randomUUID();
            var now = LocalDateTime.now();
            var event = buildEvent(ownerId, now.plusDays(1), now.plusDays(1).plusHours(3), 10);

            when(currentUserService.requireUserId()).thenReturn(userId);
            when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
            when(registrationRepository.findByEventIdAndVolunteerId(event.getId(), userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> registrationService.cancel(event.getId()))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("não encontrada");

            verify(registrationRepository, never()).save(any());
        }

        @Test
        void shouldThrowWhenRegistrationIsAlreadyCanceled() {
            var userId = UUID.randomUUID();
            var ownerId = UUID.randomUUID();
            var now = LocalDateTime.now();
            var event = buildEvent(ownerId, now.plusDays(1), now.plusDays(1).plusHours(3), 10);

            var registration = EventRegistration.builder()
                    .id(UUID.randomUUID())
                    .event(event)
                    .status(ParticipationStatus.CANCELED)
                    .build();

            when(currentUserService.requireUserId()).thenReturn(userId);
            when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
            when(registrationRepository.findByEventIdAndVolunteerId(event.getId(), userId))
                    .thenReturn(Optional.of(registration));

            assertThatThrownBy(() -> registrationService.cancel(event.getId()))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("já foi cancelada");

            verify(registrationRepository, never()).save(any());
        }
    }

    @Nested
    class GetEnrollmentStatusTest {

        @Test
        void shouldReturnTrueWhenUserHasActiveRegistration() {
            var userId = UUID.randomUUID();
            var eventId = UUID.randomUUID();

            var registration = EventRegistration.builder()
                    .id(UUID.randomUUID())
                    .status(ParticipationStatus.REGISTERED)
                    .build();

            when(currentUserService.requireUserId()).thenReturn(userId);
            when(registrationRepository.findByEventIdAndVolunteerId(eventId, userId))
                    .thenReturn(Optional.of(registration));

            var result = registrationService.getEnrollmentStatus(eventId);

            assertThat(result.enrolled()).isTrue();
        }

        @Test
        void shouldReturnFalseWhenRegistrationIsCanceled() {
            var userId = UUID.randomUUID();
            var eventId = UUID.randomUUID();

            var registration = EventRegistration.builder()
                    .id(UUID.randomUUID())
                    .status(ParticipationStatus.CANCELED)
                    .build();

            when(currentUserService.requireUserId()).thenReturn(userId);
            when(registrationRepository.findByEventIdAndVolunteerId(eventId, userId))
                    .thenReturn(Optional.of(registration));

            var result = registrationService.getEnrollmentStatus(eventId);

            assertThat(result.enrolled()).isFalse();
        }

        @Test
        void shouldReturnFalseWhenNoRegistrationExists() {
            var userId = UUID.randomUUID();
            var eventId = UUID.randomUUID();

            when(currentUserService.requireUserId()).thenReturn(userId);
            when(registrationRepository.findByEventIdAndVolunteerId(eventId, userId))
                    .thenReturn(Optional.empty());

            var result = registrationService.getEnrollmentStatus(eventId);

            assertThat(result.enrolled()).isFalse();
        }
    }

    @Nested
    class GetParticipantsTest {

        @Test
        void shouldReturnActiveParticipantsWhenCalledByOwner() {
            var ownerId = UUID.randomUUID();
            var now = LocalDateTime.now();
            var event = buildEvent(ownerId, now.plusDays(1), now.plusDays(1).plusHours(3), 10);

            var volunteer = new User();
            volunteer.setId(UUID.randomUUID());
            volunteer.setFullName("Maria Souza");
            volunteer.setEmail("maria@email.com");
            volunteer.setCpfCnpj("12345678901");
            volunteer.setPhone("47999990000");

            var activeRegistration = EventRegistration.builder()
                    .id(UUID.randomUUID())
                    .event(event)
                    .volunteer(volunteer)
                    .status(ParticipationStatus.REGISTERED)
                    .build();

            var canceledRegistration = EventRegistration.builder()
                    .id(UUID.randomUUID())
                    .event(event)
                    .volunteer(new User())
                    .status(ParticipationStatus.CANCELED)
                    .build();

            when(currentUserService.requireUserId()).thenReturn(ownerId);
            when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
            when(registrationRepository.findAllByEventIdOrderByRegisteredAtAsc(event.getId()))
                    .thenReturn(List.of(activeRegistration, canceledRegistration));

            var result = registrationService.getParticipants(event.getId());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("Maria Souza");
            assertThat(result.get(0).email()).isEqualTo("maria@email.com");
        }

        @Test
        void shouldThrowWhenCalledByNonOwner() {
            var ownerId = UUID.randomUUID();
            var otherUserId = UUID.randomUUID();
            var now = LocalDateTime.now();
            var event = buildEvent(ownerId, now.plusDays(1), now.plusDays(1).plusHours(3), 10);

            when(currentUserService.requireUserId()).thenReturn(otherUserId);
            when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

            assertThatThrownBy(() -> registrationService.getParticipants(event.getId()))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("dono do evento");

            verify(registrationRepository, never()).findAllByEventIdOrderByRegisteredAtAsc(any());
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
            var canceledEvent = buildEvent(ownerId, now.plusDays(2), now.plusDays(2).plusHours(2), 10);

            var activeRegistration = EventRegistration.builder()
                    .id(UUID.randomUUID())
                    .event(activeEvent)
                    .status(ParticipationStatus.REGISTERED)
                    .build();

            var canceledRegistration = EventRegistration.builder()
                    .id(UUID.randomUUID())
                    .event(canceledEvent)
                    .status(ParticipationStatus.CANCELED)
                    .build();

            when(currentUserService.requireUserId()).thenReturn(userId);
            when(registrationRepository.findAllByVolunteerIdOrderByRegisteredAtDesc(userId))
                    .thenReturn(List.of(activeRegistration, canceledRegistration));
            when(registrationRepository.countByEventIdAndStatusIn(eq(activeEvent.getId()), anyCollection()))
                    .thenReturn(1L);

            var result = registrationService.getMyEnrolledEvents();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(activeEvent.getId());
        }

        @Test
        void shouldReturnEmptyListWhenUserHasNoEnrollments() {
            var userId = UUID.randomUUID();

            when(currentUserService.requireUserId()).thenReturn(userId);
            when(registrationRepository.findAllByVolunteerIdOrderByRegisteredAtDesc(userId))
                    .thenReturn(List.of());

            var result = registrationService.getMyEnrolledEvents();

            assertThat(result).isEmpty();
        }
    }

    private Event buildEvent(UUID ownerId, LocalDateTime startsAt, LocalDateTime endsAt, Integer capacity) {
        var owner = new User();
        owner.setId(ownerId);

        var event = new Event();
        event.setId(UUID.randomUUID());
        event.setOwner(owner);
        event.setStartsAt(startsAt);
        event.setEndsAt(endsAt);
        event.setCapacity(capacity);
        return event;
    }
}