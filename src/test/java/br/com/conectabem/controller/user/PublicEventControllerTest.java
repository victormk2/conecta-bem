package br.com.conectabem.controller.user;

import br.com.conectabem.model.Event;
import br.com.conectabem.service.EventService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicEventControllerTest {

    @InjectMocks
    private PublicEventController publicEventController;

    @Mock
    private EventService eventService;

    @Nested
    class FindByIdTest {
        @Test
        void shouldReturnEventWhenFound() {
            var eventId = "00000000-0000-0000-0000-000000000001";
            var event = new Event();
            when(eventService.findById(eventId)).thenReturn(event);

            var response = publicEventController.findById(eventId);

            verify(eventService).findById(eventId);
            Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
            Assertions.assertThat(response.getBody()).isEqualTo(event);
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
}

