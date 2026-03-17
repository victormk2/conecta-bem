package br.com.conectabem.service;

import br.com.conectabem.dto.event.CreateEventRequest;
import br.com.conectabem.dto.event.EventReportDTO;
import br.com.conectabem.dto.event.UpdateEventRequest;
import br.com.conectabem.model.Event;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventService {

    Event create(CreateEventRequest request);

    List<Event> listMine();

    List<Event> findAvailable(String location, String activityType, String fromDate);

    Optional<Event> findOwnedById(UUID eventId);

    Optional<Event> findAccessibleById(UUID eventId);

    Optional<Event> update(UUID eventId, UpdateEventRequest request);

    boolean delete(UUID eventId);

    EventReportDTO buildReport(UUID eventId);

    long getAvailableSpots(UUID eventId);
}
