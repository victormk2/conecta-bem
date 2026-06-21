package br.com.conectabem.dto.event;

import br.com.conectabem.model.Address;
import br.com.conectabem.model.EventCategory;
import br.com.conectabem.model.EventType;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventResponse(
        UUID id,
        UUID ownerId,
        String title,
        String description,
        Address address,
        EventCategory category,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        Integer capacity,
        long enrolledCount,
        String imageUrl,
        EventType type,
        String organizationName,
        String organizationDocument
) {
    public EventResponse(
            UUID id,
            UUID ownerId,
            String title,
            String description,
            Address address,
            EventCategory category,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            Integer capacity,
            long enrolledCount,
            String imageUrl
    ) {
        this(id, ownerId, title, description, address, category, startsAt, endsAt, capacity, enrolledCount, imageUrl, EventType.COMMUNITY, null, null);
    }
}
