package br.com.conectabem.controller;

import br.com.conectabem.dto.eventregistration.EventRegistrationCreateRequest;
import br.com.conectabem.dto.eventregistration.EventRegistrationDecisionRequest;
import br.com.conectabem.dto.eventregistration.EventRegistrationResponse;
import br.com.conectabem.service.EventRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/event-registrations")
@RequiredArgsConstructor
public class EventRegistrationController {

    private final EventRegistrationService eventRegistrationService;

    @PostMapping
    public ResponseEntity<EventRegistrationResponse> register(@RequestBody EventRegistrationCreateRequest request) {
        var response = eventRegistrationService.register(request.eventId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/confirm")
    public EventRegistrationResponse confirm(@PathVariable String id) {
        return eventRegistrationService.confirm(id);
    }

    @PatchMapping("/{id}/reject")
    public EventRegistrationResponse reject(
            @PathVariable String id,
            @RequestBody(required = false) EventRegistrationDecisionRequest request
    ) {
        return eventRegistrationService.reject(id, request);
    }

    @PatchMapping("/{id}/dismiss")
    public EventRegistrationResponse dismiss(
            @PathVariable String id,
            @RequestBody(required = false) EventRegistrationDecisionRequest request
    ) {
        return eventRegistrationService.dismiss(id, request);
    }

    @GetMapping("/events/{eventId}")
    public List<EventRegistrationResponse> listByEvent(@PathVariable String eventId) {
        return eventRegistrationService.listByEvent(eventId);
    }

    @GetMapping("/me")
    public List<EventRegistrationResponse> listCurrentUserRegistrations() {
        return eventRegistrationService.listCurrentUserRegistrations();
    }
}
