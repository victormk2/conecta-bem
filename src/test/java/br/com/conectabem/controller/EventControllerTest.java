package br.com.conectabem.controller;

import br.com.conectabem.dto.event.EventListRequest;
import br.com.conectabem.dto.event.EventCreationDTO;
import br.com.conectabem.dto.event.EventListResponse;
import br.com.conectabem.dto.event.EventResponse;
import br.com.conectabem.model.Event;
import br.com.conectabem.model.EventCategory;
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
class EventControllerTest {

    @InjectMocks
    private EventController eventController;
    @Mock
    private EventService eventService;

    @Nested
    class ListTest {
        @Test
        void shouldCallService() {
            var input = new EventListRequest();
            when(eventService.list(input)).thenReturn(new EventListResponse(List.of(), 0L));

            var response = eventController.list(input);

            verify(eventService).list(input);
            Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
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

            var response = eventController.findById(eventId);

            verify(eventService).findById(eventId);
            verify(eventService).toEventResponse(event);
            Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
            Assertions.assertThat(response.getBody()).isEqualTo(eventResponse);
        }

        @Test
        void shouldReturnNotFoundWhenEventDoesNotExist() {
            var eventId = "00000000-0000-0000-0000-000000000001";
            when(eventService.findById(eventId)).thenReturn(null);

            var response = eventController.findById(eventId);

            verify(eventService).findById(eventId);
            Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(404));
        }
    }

    @Nested
    class CreateTest {
        @Test
        void shouldCallService() {
            var input = new EventCreationDTO(
                    "Mutirao",
                    "Descricao",
                    "00000000-0000-0000-0000-000000000001",
                    "SOCIAL",
                    "2026-04-15T10:00:00",
                    "2026-04-15T14:00:00",
                    10
            );
            when(eventService.createWithImage(input, null)).thenReturn(new Event());

            var response = eventController.create(input, null);

            verify(eventService).createWithImage(input, null);
            Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(201));
        }
    }

    @Nested
    class RemoveImageTest {
        @Test
        void shouldReturnNoContentWhenImageIsRemoved() {
            var eventId = "00000000-0000-0000-0000-000000000001";
            when(eventService.removeImageByEventId(eventId)).thenReturn(true);

            var response = eventController.removeImage(eventId);

            verify(eventService).removeImageByEventId(eventId);
            Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(204));
        }

        @Test
        void shouldReturnNotFoundWhenEventDoesNotExist() {
            var eventId = "00000000-0000-0000-0000-000000000001";
            when(eventService.removeImageByEventId(eventId)).thenReturn(false);

            var response = eventController.removeImage(eventId);

            verify(eventService).removeImageByEventId(eventId);
            Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(404));
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