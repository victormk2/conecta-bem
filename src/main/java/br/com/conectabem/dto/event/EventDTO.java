package br.com.conectabem.dto.event;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class EventDTO {
    private UUID id;
    private String title;
    private String description;
    private String location;
    private String activityType;
    private Instant startsAt;
    private Instant endsAt;
    private Integer capacity;
    private long availableSpots;
    private UUID ownerId;
    private Instant createdAt;
    private Instant updatedAt;
}
