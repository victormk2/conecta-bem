package br.com.conectabem.controller;

import br.com.conectabem.dto.event.EnrollmentStatusDTO;
import br.com.conectabem.dto.event.ParticipantDTO;
import br.com.conectabem.service.EventRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventRegistrationController {

    private final EventRegistrationService registrationService;

    @PostMapping("/{id}/enroll")
    public ResponseEntity<Void> enroll(@PathVariable UUID id) {
        registrationService.enroll(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/enroll")
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        registrationService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/enrollment/status")
    public ResponseEntity<EnrollmentStatusDTO> enrollmentStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(registrationService.getEnrollmentStatus(id));
    }

    /**
     * GET /events/{id}/enrollments
     * Returns all active participants (restricted to event owner).
     */
    @GetMapping("/{id}/enrollments")
    public ResponseEntity<List<ParticipantDTO>> participants(@PathVariable UUID id) {
        return ResponseEntity.ok(registrationService.getParticipants(id));
    }
}