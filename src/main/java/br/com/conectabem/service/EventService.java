package br.com.conectabem.service;

import br.com.conectabem.dto.event.EventCreationDTO;
import br.com.conectabem.dto.event.EventListRequest;
import br.com.conectabem.dto.event.EventListResponse;
import br.com.conectabem.model.Event;

public interface EventService {

    Event create(EventCreationDTO eventCreationDTO);

    EventListResponse list(EventListRequest eventListRequest);
}
