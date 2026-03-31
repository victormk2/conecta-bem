package br.com.conectabem.dto.event;

import br.com.conectabem.model.Event;

import java.util.List;

public record EventListResponse(
        List<Event> events,
        Long total
) {
}
