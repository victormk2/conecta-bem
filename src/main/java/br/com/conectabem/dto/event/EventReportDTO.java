package br.com.conectabem.dto.event;

import lombok.Data;

import java.util.Map;

@Data
public class EventReportDTO {
    private EventDTO event;
    private int totalRegistrations;
    private long availableSpots;
    private Map<String, Long> countsByStatus;
}
