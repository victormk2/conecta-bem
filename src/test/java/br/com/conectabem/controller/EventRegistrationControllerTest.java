package br.com.conectabem.controller;

import br.com.conectabem.dto.event.EventRegistrationDTO;
import br.com.conectabem.dto.event.JustifyAbsenceRequest;
import br.com.conectabem.dto.event.UpdateParticipationStatusRequest;
import br.com.conectabem.model.ParticipationStatus;
import br.com.conectabem.service.EventRegistrationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventRegistrationControllerTest {

    @Mock
    private EventRegistrationService registrationService;

    private EventRegistrationController controller;

    @BeforeEach
    void setup() {
        controller = new EventRegistrationController(registrationService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("localhost");
        request.setServerPort(8080);
        request.setScheme("http");
        request.setRequestURI("/events");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void registerReturnsCreatedParticipation() {
        UUID eventId = UUID.randomUUID();
        EventRegistrationDTO dto = new EventRegistrationDTO();
        dto.setId(UUID.randomUUID());
        dto.setEventId(eventId);
        dto.setStatus(ParticipationStatus.REGISTERED);
        when(registrationService.register(eventId)).thenReturn(dto);

        ResponseEntity<EventRegistrationDTO> response = controller.register(eventId);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(ParticipationStatus.REGISTERED, response.getBody().getStatus());
    }

    @Test
    void cancelRegistrationReturnsNoContentWhenCanceled() {
        UUID eventId = UUID.randomUUID();
        when(registrationService.cancelRegistration(eventId)).thenReturn(true);

        ResponseEntity<Void> response = controller.cancelRegistration(eventId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void cancelRegistrationReturnsNotFoundWhenNothingToCancel() {
        UUID eventId = UUID.randomUUID();
        when(registrationService.cancelRegistration(eventId)).thenReturn(false);

        ResponseEntity<Void> response = controller.cancelRegistration(eventId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void listParticipantsReturnsRegistrations() {
        UUID eventId = UUID.randomUUID();
        EventRegistrationDTO dto = new EventRegistrationDTO();
        dto.setVolunteerId(UUID.randomUUID());
        when(registrationService.listParticipants(eventId)).thenReturn(List.of(dto));

        ResponseEntity<List<EventRegistrationDTO>> response = controller.listParticipants(eventId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void updateParticipationStatusReturnsUpdatedRegistration() {
        UUID eventId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();
        UpdateParticipationStatusRequest request = new UpdateParticipationStatusRequest();
        request.setStatus(ParticipationStatus.PRESENT);
        EventRegistrationDTO dto = new EventRegistrationDTO();
        dto.setStatus(ParticipationStatus.PRESENT);
        when(registrationService.updateParticipationStatus(eventId, volunteerId, request)).thenReturn(dto);

        ResponseEntity<EventRegistrationDTO> response =
                controller.updateParticipationStatus(eventId, volunteerId, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(ParticipationStatus.PRESENT, response.getBody().getStatus());
    }

    @Test
    void justifyAbsenceReturnsUpdatedRegistrationForSameUser() {
        UUID eventId = UUID.randomUUID();
        JustifyAbsenceRequest request = new JustifyAbsenceRequest();
        request.setJustification("Motivo");
        EventRegistrationDTO dto = new EventRegistrationDTO();
        dto.setStatus(ParticipationStatus.JUSTIFIED);
        when(registrationService.justifyAbsence(eventId, request)).thenReturn(dto);

        ResponseEntity<EventRegistrationDTO> response =
                controller.justifyAbsence(eventId, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(ParticipationStatus.JUSTIFIED, response.getBody().getStatus());
    }

    @Test
    void myRegistrationsReturnsHistory() {
        EventRegistrationDTO dto = new EventRegistrationDTO();
        when(registrationService.myRegistrations(true)).thenReturn(List.of(dto));

        ResponseEntity<List<EventRegistrationDTO>> response = controller.myRegistrations(true);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }
}
