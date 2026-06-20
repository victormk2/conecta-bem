package br.com.conectabem.controller;

import br.com.conectabem.dto.event.EnrollmentStatusDTO;
import br.com.conectabem.dto.event.ParticipantDTO;
import br.com.conectabem.dto.eventregistration.EventRegistrationDecisionRequest;
import br.com.conectabem.dto.eventregistration.EventRegistrationResponse;
import br.com.conectabem.service.EventRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class EventRegistrationController {

    private final EventRegistrationService registrationService;

    @PostMapping("/events/{id}/enroll")
    public ResponseEntity<Void> enroll(@PathVariable UUID id) {
        registrationService.enroll(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/events/{id}/enroll")
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        registrationService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/events/{id}/enrollment/status")
    public ResponseEntity<EnrollmentStatusDTO> enrollmentStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(registrationService.getEnrollmentStatus(id));
    }

    @GetMapping("/events/{id}/enrollments")
    public ResponseEntity<List<ParticipantDTO>> participants(@PathVariable UUID id) {
        return ResponseEntity.ok(registrationService.getParticipants(id));
    }

    @PatchMapping("/event-registrations/{id}/confirm")
    public EventRegistrationResponse confirm(@PathVariable UUID id) {
        return registrationService.confirm(id);
    }

    @PatchMapping("/event-registrations/{id}/reject")
    public EventRegistrationResponse reject(
            @PathVariable UUID id,
            @RequestBody(required = false) EventRegistrationDecisionRequest request
    ) {
        return registrationService.reject(id, request);
    }

    @PatchMapping("/event-registrations/{id}/dismiss")
    public EventRegistrationResponse dismiss(
            @PathVariable UUID id,
            @RequestBody(required = false) EventRegistrationDecisionRequest request
    ) {
        return registrationService.dismiss(id, request);
    }

    @GetMapping("/event-registrations/events/{eventId}")
    public List<EventRegistrationResponse> listByEvent(@PathVariable UUID eventId) {
        return registrationService.listByEvent(eventId);
    }
}
