package br.com.conectabem.service;

import br.com.conectabem.dto.event.EventCreationDTO;
import br.com.conectabem.dto.event.EventListRequest;
import br.com.conectabem.dto.event.EventListResponse;
import br.com.conectabem.dto.event.EventResponse;
import br.com.conectabem.dto.event.EventUpdateDTO;
import br.com.conectabem.model.Event;
import org.springframework.web.multipart.MultipartFile;

public interface EventService {

    Event create(EventCreationDTO eventCreationDTO);

    Event createWithImage(EventCreationDTO eventCreationDTO, MultipartFile image);

    Event update(EventUpdateDTO eventUpdateDTO);

    Event updateWithImage(EventUpdateDTO eventUpdateDTO, MultipartFile image);

    boolean removeImageByEventId(String eventId);

    /** Returns the raw entity (used internally / by authenticated event owner endpoints). */
    Event findById(String eventId);

    /**
     * Returns the public list of events, excluding finished ones,
     * with live enrolledCount per event.
     */
    EventListResponse list(EventListRequest eventListRequest);

    /** Converts a single Event entity to its public DTO (with live enrolledCount). */
    EventResponse toEventResponse(Event event);
}