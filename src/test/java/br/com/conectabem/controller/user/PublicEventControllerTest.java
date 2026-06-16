package br.com.conectabem.controller.user;

import br.com.conectabem.dto.event.EventListRequest;
import br.com.conectabem.dto.event.EventListResponse;
import br.com.conectabem.dto.event.EventResponse;
import br.com.conectabem.model.Event;
import br.com.conectabem.model.EventCategory;
import br.com.conectabem.service.EventRegistrationService;
import br.com.conectabem.service.EventService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicEventControllerTest {

    @InjectMocks
    private PublicEventController publicEventController;

    @Mock
    private EventService eventService;

    @Mock
    private EventRegistrationService registrationService;

    @Nested
    class ListTest {
        @Test
        void shouldCallService() {
            var input = new EventListRequest();
            when(eventService.list(input)).thenReturn(new EventListResponse(List.of(), 0L));

            var response = publicEventController.list(input);

            verify(eventService).list(input);
            Assertions.assertThat(response.events()).isEmpty();
            Assertions.assertThat(response.total()).isEqualTo(0L);
        }
    }

    @Nested
    class FindByIdTest {
        @Test
        void shouldReturnEventResponseWhenFound() {
            var eventId = "00000000-0000-0000-0000-000000000001";
            var event = new Event();
            event.setId(UUID.fromString(eventId));

            var eventResponse = buildEventResponse(UUID.fromString(eventId));

            when(eventService.findById(eventId)).thenReturn(event);
            when(eventService.toEventResponse(event)).thenReturn(eventResponse);

            var response = publicEventController.findById(eventId);

            verify(eventService).findById(eventId);
            verify(eventService).toEventResponse(event);
            Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
            Assertions.assertThat(response.getBody()).isEqualTo(eventResponse);
        }

        @Test
        void shouldReturnNotFoundWhenEventDoesNotExist() {
            var eventId = "00000000-0000-0000-0000-000000000001";
            when(eventService.findById(eventId)).thenReturn(null);

            var response = publicEventController.findById(eventId);

            verify(eventService).findById(eventId);
            Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(404));
        }
    }

    @Nested
    class MyEnrolledEventsTest {
        @Test
        void shouldReturnEnrolledEventsFromRegistrationService() {
            var eventResponse = buildEventResponse(UUID.randomUUID());
            when(registrationService.getMyEnrolledEvents()).thenReturn(List.of(eventResponse));

            var response = publicEventController.myEnrolledEvents();

            verify(registrationService).getMyEnrolledEvents();
            Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
            Assertions.assertThat(response.getBody()).containsExactly(eventResponse);
        }

        @Test
        void shouldReturnEmptyListWhenUserHasNoEnrollments() {
            when(registrationService.getMyEnrolledEvents()).thenReturn(List.of());

            var response = publicEventController.myEnrolledEvents();

            verify(registrationService).getMyEnrolledEvents();
            Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
            Assertions.assertThat(response.getBody()).isEmpty();
        }
    }

    private EventResponse buildEventResponse(UUID id) {
        return new EventResponse(
                id,
                UUID.randomUUID(),
                "Mutirao",
                "Descricao",
                null,
                EventCategory.SOCIAL,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(4),
                10,
                0L,
                null
        );
    }
}