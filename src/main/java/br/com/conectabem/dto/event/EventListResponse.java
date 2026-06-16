package br.com.conectabem.dto.event;

import java.util.List;

public record EventListResponse(
        List<EventResponse> events,
        Long total
) {}