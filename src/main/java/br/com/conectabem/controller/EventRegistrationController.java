package br.com.conectabem.controller;

import br.com.conectabem.dto.event.EventRegistrationDTO;
import br.com.conectabem.dto.event.JustifyAbsenceRequest;
import br.com.conectabem.dto.event.UpdateParticipationStatusRequest;
import br.com.conectabem.service.EventRegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    public ResponseEntity<EventRegistrationDTO> register(@PathVariable UUID id, Authentication authentication) {
        UUID userId = requireUserId(authentication);
        EventRegistrationDTO registration = service.register(id, userId);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/me")
                .build()
                .toUri();
        return ResponseEntity.created(location).body(registration);
    }

    @DeleteMapping("/{id}/registrations/me")
    public ResponseEntity<Void> cancelRegistration(@PathVariable UUID id, Authentication authentication) {
        UUID userId = requireUserId(authentication);
        return service.cancelRegistration(id, userId)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/participants")
    public ResponseEntity<List<EventRegistrationDTO>> listParticipants(@PathVariable UUID id,
                                                                       Authentication authentication) {
        UUID userId = requireUserId(authentication);
        return ResponseEntity.ok(service.listParticipants(id, userId));
    }

    @PatchMapping("/{id}/participants/{volunteerId}/status")
    public ResponseEntity<EventRegistrationDTO> updateParticipationStatus(@PathVariable UUID id,
                                                                          @PathVariable UUID volunteerId,
                                                                          @Valid @RequestBody UpdateParticipationStatusRequest request,
                                                                          Authentication authentication) {
        UUID userId = requireUserId(authentication);
        return ResponseEntity.ok(service.updateParticipationStatus(id, volunteerId, request, userId));
    }

    @PostMapping("/{id}/participants/{volunteerId}/justification")
    public ResponseEntity<EventRegistrationDTO> justifyAbsence(@PathVariable UUID id,
                                                               @PathVariable UUID volunteerId,
                                                               @Valid @RequestBody JustifyAbsenceRequest request,
                                                               Authentication authentication) {
        UUID userId = requireUserId(authentication);
        if (!userId.equals(volunteerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(service.justifyAbsence(id, volunteerId, request));
    }

    @GetMapping("/registrations/me")
    public ResponseEntity<List<EventRegistrationDTO>> myRegistrations(@RequestParam(defaultValue = "false") boolean futureOnly,
                                                                      Authentication authentication) {
        UUID userId = requireUserId(authentication);
        return ResponseEntity.ok(service.listVolunteerHistory(userId, futureOnly));
    }

    private UUID requireUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("authentication required");
        }
        return (UUID) authentication.getPrincipal();
    }
}
