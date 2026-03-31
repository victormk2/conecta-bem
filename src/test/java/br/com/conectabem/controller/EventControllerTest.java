package br.com.conectabem.controller;

import br.com.conectabem.dto.event.EventListRequest;
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
            var response = eventController.list(input);
            verify(eventService).list(input);
            Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
        }
    }

    @Nested
    class CreateTest {
        @Test
        void shouldCallService() {
            var input = new EventListRequest();
            var response = eventController.list(input);
            verify(eventService).list(input);
            Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
        }
    }
}