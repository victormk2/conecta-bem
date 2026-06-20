package br.com.conectabem.controller;

import br.com.conectabem.dto.eventregistration.EventRegistrationCreateRequest;
import br.com.conectabem.dto.eventregistration.EventRegistrationDecisionRequest;
import br.com.conectabem.dto.eventregistration.EventRegistrationResponse;
import br.com.conectabem.service.EventRegistrationService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventRegistrationControllerTest {

    @InjectMocks
    private EventRegistrationController controller;

    @Mock
    private EventRegistrationService service;

    @Nested
    class RegisterTest {
        @Test
        void shouldCallServiceAndReturnCreated() {
            var request = new EventRegistrationCreateRequest("00000000-0000-0000-0000-000000000001");
            var responseBody = response("PENDING");
            when(service.register(request.eventId())).thenReturn(responseBody);

            var response = controller.register(request);

            verify(service).register(request.eventId());
            Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(201));
            Assertions.assertThat(response.getBody()).isEqualTo(responseBody);
        }
    }

    @Nested
    class DecisionTest {
        @Test
        void shouldCallReject() {
            var request = new EventRegistrationDecisionRequest("Sem vaga");
            var response = response("REJECTED");
            when(service.reject("registration-id", request)).thenReturn(response);

            var result = controller.reject("registration-id", request);

            verify(service).reject("registration-id", request);
            Assertions.assertThat(result).isEqualTo(response);
        }

        @Test
        void shouldCallDismiss() {
            var request = new EventRegistrationDecisionRequest("Equipe completa");
            var response = response("DISMISSED");
            when(service.dismiss("registration-id", request)).thenReturn(response);

            var result = controller.dismiss("registration-id", request);

            verify(service).dismiss("registration-id", request);
            Assertions.assertThat(result).isEqualTo(response);
        }
    }

    @Nested
    class ListTest {
        @Test
        void shouldCallListByEvent() {
            var registrations = List.of(response("PENDING"));
            when(service.listByEvent("event-id")).thenReturn(registrations);

            var result = controller.listByEvent("event-id");

            verify(service).listByEvent("event-id");
            Assertions.assertThat(result).isEqualTo(registrations);
        }
    }

    private static EventRegistrationResponse response(String status) {
        return new EventRegistrationResponse(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                UUID.fromString("00000000-0000-0000-0000-000000000003"),
                status,
                null,
                null,
                null
        );
    }
}
