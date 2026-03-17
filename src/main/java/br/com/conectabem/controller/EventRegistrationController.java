package br.com.conectabem.controller;

import br.com.conectabem.dto.event.EventRegistrationDTO;
import br.com.conectabem.dto.event.JustifyAbsenceRequest;
import br.com.conectabem.dto.event.UpdateParticipationStatusRequest;
import br.com.conectabem.service.EventRegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/events")
public class EventRegistrationController {

    private final EventRegistrationService service;

    public EventRegistrationController(EventRegistrationService service) {
        this.service = service;
    }

    @PostMapping("/{id}/registrations")
    public ResponseEntity<EventRegistrationDTO> register(@PathVariable UUID id) {
        EventRegistrationDTO registration = service.register(id);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/me")
                .build()
                .toUri();
        return ResponseEntity.created(location).body(registration);
    }

    @DeleteMapping("/{id}/registrations/me")
    public ResponseEntity<Void> cancelRegistration(@PathVariable UUID id) {
        return service.cancelRegistration(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/participants")
    public ResponseEntity<List<EventRegistrationDTO>> listParticipants(@PathVariable UUID id) {
        return ResponseEntity.ok(service.listParticipants(id));
    }

    @PatchMapping("/{id}/participants/{volunteerId}/status")
    public ResponseEntity<EventRegistrationDTO> updateParticipationStatus(@PathVariable UUID id,
                                                                          @PathVariable UUID volunteerId,
                                                                          @Valid @RequestBody UpdateParticipationStatusRequest request) {
        return ResponseEntity.ok(service.updateParticipationStatus(id, volunteerId, request));
    }

    @PostMapping("/{id}/registrations/me/justification")
    public ResponseEntity<EventRegistrationDTO> justifyAbsence(@PathVariable UUID id,
                                                               @Valid @RequestBody JustifyAbsenceRequest request) {
        return ResponseEntity.ok(service.justifyAbsence(id, request));
    }

    @GetMapping("/registrations/me")
    public ResponseEntity<List<EventRegistrationDTO>> myRegistrations(@RequestParam(defaultValue = "false") boolean futureOnly) {
        return ResponseEntity.ok(service.myRegistrations(futureOnly));
    }
}
