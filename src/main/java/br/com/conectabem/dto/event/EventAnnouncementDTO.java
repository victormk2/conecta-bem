package br.com.conectabem.dto.event;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class EventAnnouncementDTO {
    private UUID id;
    private UUID eventId;
    private UUID authorId;
    private Instant createdAt;
    private String message;
}
