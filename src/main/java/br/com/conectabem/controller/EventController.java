package br.com.conectabem.controller;

import br.com.conectabem.dto.event.EventCreationDTO;
import br.com.conectabem.dto.event.EventListRequest;
import br.com.conectabem.dto.event.EventListResponse;
import br.com.conectabem.dto.event.EventResponse;
import br.com.conectabem.dto.event.EventUpdateDTO;
import br.com.conectabem.model.Event;
import br.com.conectabem.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<EventListResponse> list(@ModelAttribute EventListRequest request) {
        return ResponseEntity.ok(eventService.list(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> findById(@PathVariable String id) {
        Event event = eventService.findById(id);
        if (event == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(eventService.toEventResponse(event));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EventResponse> create(
            @RequestPart("event") EventCreationDTO eventCreationDTO,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        Event event = eventService.createWithImage(eventCreationDTO, image);
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.toEventResponse(event));
    }

    @PatchMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EventResponse> update(
            @RequestPart("event") EventUpdateDTO eventUpdateDTO,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        var event = eventService.updateWithImage(eventUpdateDTO, image);
        if (event == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.status(HttpStatus.OK).body(eventService.toEventResponse(event));
    }

    @DeleteMapping("/{id}/image")
    public ResponseEntity<Void> removeImage(@PathVariable String id) {
        boolean removed = eventService.removeImageByEventId(id);
        if (!removed) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.noContent().build();
    }
}
