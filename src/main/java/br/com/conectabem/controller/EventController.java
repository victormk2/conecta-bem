package br.com.conectabem.controller;

import br.com.conectabem.dto.event.EventCreationDTO;
import br.com.conectabem.dto.event.EventListRequest;
import br.com.conectabem.dto.event.EventListResponse;
import br.com.conectabem.model.Event;
import br.com.conectabem.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<EventListResponse> list(@ModelAttribute EventListRequest request) {
        EventListResponse response = eventService.list(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Event> create(@RequestBody EventCreationDTO eventCreationDTO) {
        Event event = eventService.create(eventCreationDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(event);
    }
}
