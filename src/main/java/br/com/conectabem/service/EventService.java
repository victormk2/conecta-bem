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

    Event findById(String eventId);

    EventListResponse list(EventListRequest eventListRequest);

    EventResponse toEventResponse(Event event);
}