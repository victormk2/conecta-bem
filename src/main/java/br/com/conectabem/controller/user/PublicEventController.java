package br.com.conectabem.controller.user;

import br.com.conectabem.dto.event.EventListRequest;
import br.com.conectabem.dto.event.EventListResponse;
import br.com.conectabem.dto.event.EventResponse;
import br.com.conectabem.service.EventRegistrationService;
import br.com.conectabem.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/events")
@RequiredArgsConstructor
public class PublicEventController {

    private final EventService eventService;
    private final EventRegistrationService registrationService;

    @GetMapping
    public EventListResponse list(@ModelAttribute EventListRequest request) {
        return eventService.list(request);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> findById(@PathVariable String id) {
        var event = eventService.findById(id);
        if (event == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(eventService.toEventResponse(event));
    }

    @GetMapping("/enrolled")
    public ResponseEntity<List<EventResponse>> myEnrolledEvents() {
        return ResponseEntity.ok(registrationService.getMyEnrolledEvents());
    }
}