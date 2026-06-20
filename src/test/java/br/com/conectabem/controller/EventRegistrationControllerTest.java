package br.com.conectabem.controller;

import br.com.conectabem.dto.event.EnrollmentStatusDTO;
import br.com.conectabem.dto.event.ParticipantDTO;
import br.com.conectabem.dto.eventregistration.EventRegistrationDecisionRequest;
import br.com.conectabem.dto.eventregistration.EventRegistrationResponse;
import br.com.conectabem.model.Gender;
import br.com.conectabem.service.EventRegistrationService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventRegistrationControllerTest {

    @InjectMocks
    private EventRegistrationController controller;

    @Mock
    private EventRegistrationService registrationService;

    @Nested
    class EnrollTest {
        @Test
        void shouldCallServiceAndReturnOk() {
            var eventId = UUID.randomUUID();

            var response = controller.enroll(eventId);

            verify(registrationService).enroll(eventId);
            Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
        }
    }

    @Nested
    class CancelTest {
        @Test
        void shouldCallServiceAndReturnNoContent() {
            var eventId = UUID.randomUUID();

            var response = controller.cancel(eventId);

            verify(registrationService).cancel(eventId);
            Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(204));
        }
    }

    @Nested
    class EnrollmentStatusTest {
        @Test
        void shouldReturnEnrollmentStatusFromService() {
            var eventId = UUID.randomUUID();
            var status = new EnrollmentStatusDTO(true);

            when(registrationService.getEnrollmentStatus(eventId)).thenReturn(status);

            var response = controller.enrollmentStatus(eventId);

            verify(registrationService).getEnrollmentStatus(eventId);
            Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
            Assertions.assertThat(response.getBody()).isEqualTo(status);
        }
    }

    @Nested
    class ParticipantsTest {
        @Test
        void shouldReturnParticipantListFromService() {
            var eventId = UUID.randomUUID();
            var participant = new ParticipantDTO(
                    UUID.randomUUID(),
                    "Maria Souza",
                    "maria@email.com",
                    "12345678901",
                    LocalDate.of(1995, 5, 20),
                    "47999990000",
                    Gender.FEMALE
            );

            when(registrationService.getParticipants(eventId)).thenReturn(List.of(participant));

            var response = controller.participants(eventId);

            verify(registrationService).getParticipants(eventId);
            Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
            Assertions.assertThat(response.getBody()).containsExactly(participant);
        }
    }

    @Nested
    class DecisionTest {
        @Test
        void shouldCallReject() {
            var registrationId = UUID.randomUUID();
            var request = new EventRegistrationDecisionRequest("Sem vaga");
            var response = response("REJECTED");
            when(registrationService.reject(registrationId, request)).thenReturn(response);

            var result = controller.reject(registrationId, request);

            verify(registrationService).reject(registrationId, request);
            Assertions.assertThat(result).isEqualTo(response);
        }

        @Test
        void shouldCallDismiss() {
            var registrationId = UUID.randomUUID();
            var request = new EventRegistrationDecisionRequest("Equipe completa");
            var response = response("DISMISSED");
            when(registrationService.dismiss(registrationId, request)).thenReturn(response);

            var result = controller.dismiss(registrationId, request);

            verify(registrationService).dismiss(registrationId, request);
            Assertions.assertThat(result).isEqualTo(response);
        }
    }

    @Nested
    class ListTest {
        @Test
        void shouldCallListByEvent() {
            var eventId = UUID.randomUUID();
            var registrations = List.of(response("PENDING"));
            when(registrationService.listByEvent(eventId)).thenReturn(registrations);

            var result = controller.listByEvent(eventId);

            verify(registrationService).listByEvent(eventId);
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
