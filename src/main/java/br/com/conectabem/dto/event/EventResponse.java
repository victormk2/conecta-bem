package br.com.conectabem.dto.event;

import br.com.conectabem.model.Address;
import br.com.conectabem.model.EventCategory;

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
        String imageUrl   // base64 data-URI, or null
) {}