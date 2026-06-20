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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
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
    private UserService userService;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private EventRegistrationServiceImpl service;

    @Nested
    class RegisterTest {
        @Test
        void shouldCreatePendingRegistrationForCurrentUser() {
            var eventId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            var ownerId = UUID.fromString("00000000-0000-0000-0000-000000000002");
            var volunteerId = UUID.fromString("00000000-0000-0000-0000-000000000003");
            var owner = user(ownerId);
            var volunteer = user(volunteerId);
            var event = event(eventId, owner);
            event.setCapacity(10);

            when(currentUserService.requireUserId()).thenReturn(volunteerId);
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(userService.findById(volunteerId)).thenReturn(volunteer);
            when(registrationRepository.findByEventIdAndVolunteerId(eventId, volunteerId)).thenReturn(Optional.empty());
            when(registrationRepository.countByEventIdAndStatusIn(any(UUID.class), anyCollection())).thenReturn(0L);
            when(registrationRepository.save(any(EventRegistration.class))).thenAnswer(invocation -> {
                EventRegistration registration = invocation.getArgument(0);
                registration.setId(UUID.fromString("00000000-0000-0000-0000-000000000004"));
                return registration;
            });

            var result = service.register(eventId.toString());

            assertThat(result.eventId()).isEqualTo(eventId);
            assertThat(result.volunteerId()).isEqualTo(volunteerId);
            assertThat(result.status()).isEqualTo("PENDING");
            verify(registrationRepository).save(any(EventRegistration.class));
        }

        @Test
        void shouldRejectOwnerRegistration() {
            var eventId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            var ownerId = UUID.fromString("00000000-0000-0000-0000-000000000002");
            var owner = user(ownerId);
            var event = event(eventId, owner);

            when(currentUserService.requireUserId()).thenReturn(ownerId);
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(userService.findById(ownerId)).thenReturn(owner);

            assertThatThrownBy(() -> service.register(eventId.toString()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("event owner cannot register as volunteer");

            verify(registrationRepository, never()).save(any(EventRegistration.class));
        }
    }

    @Nested
    class DecisionTest {
        @Test
        void shouldRejectPendingRegistrationWhenCurrentUserOwnsEvent() {
            var ownerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            var registration = registration(ParticipationStatus.PENDING, ownerId);
            var request = new EventRegistrationDecisionRequest("Nao atende ao perfil da acao");

            when(currentUserService.requireUserId()).thenReturn(ownerId);
            when(registrationRepository.findById(registration.getId())).thenReturn(Optional.of(registration));
            when(registrationRepository.save(registration)).thenReturn(registration);

            var result = service.reject(registration.getId().toString(), request);

            assertThat(result.status()).isEqualTo("REJECTED");
            assertThat(result.justification()).isEqualTo("Nao atende ao perfil da acao");
            assertThat(registration.getStatusUpdatedAt()).isNotNull();
        }

        @Test
        void shouldDismissConfirmedRegistrationWhenCurrentUserOwnsEvent() {
            var ownerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            var registration = registration(ParticipationStatus.CONFIRMED, ownerId);

            when(currentUserService.requireUserId()).thenReturn(ownerId);
            when(registrationRepository.findById(registration.getId())).thenReturn(Optional.of(registration));
            when(registrationRepository.save(registration)).thenReturn(registration);

            var result = service.dismiss(
                    registration.getId().toString(),
                    new EventRegistrationDecisionRequest("Equipe completa")
            );

            assertThat(result.status()).isEqualTo("DISMISSED");
            assertThat(result.justification()).isEqualTo("Equipe completa");
        }

        @Test
        void shouldRejectDecisionWhenCurrentUserDoesNotOwnEvent() {
            var ownerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            var currentUserId = UUID.fromString("00000000-0000-0000-0000-000000000002");
            var registration = registration(ParticipationStatus.PENDING, ownerId);

            when(currentUserService.requireUserId()).thenReturn(currentUserId);
            when(registrationRepository.findById(registration.getId())).thenReturn(Optional.of(registration));

            assertThatThrownBy(() -> service.reject(registration.getId().toString(), null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("only the event owner can manage registrations");

            verify(registrationRepository, never()).save(any(EventRegistration.class));
        }
    }

    @Nested
    class ListTest {
        @Test
        void shouldListCurrentUserRegistrations() {
            var volunteerId = UUID.fromString("00000000-0000-0000-0000-000000000003");
            var registration = registration(ParticipationStatus.PENDING, UUID.fromString("00000000-0000-0000-0000-000000000001"));
            registration.setVolunteer(user(volunteerId));

            when(currentUserService.requireUserId()).thenReturn(volunteerId);
            when(registrationRepository.findAllByVolunteerIdOrderByRegisteredAtDesc(volunteerId))
                    .thenReturn(List.of(registration));

            var result = service.listCurrentUserRegistrations();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).volunteerId()).isEqualTo(volunteerId);
        }
    }

    private static EventRegistration registration(ParticipationStatus status, UUID ownerId) {
        var registration = new EventRegistration();
        registration.setId(UUID.fromString("00000000-0000-0000-0000-000000000010"));
        registration.setEvent(event(UUID.fromString("00000000-0000-0000-0000-000000000011"), user(ownerId)));
        registration.setVolunteer(user(UUID.fromString("00000000-0000-0000-0000-000000000012")));
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
