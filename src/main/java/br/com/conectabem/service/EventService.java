package br.com.conectabem.service;

import br.com.conectabem.dto.event.CreateEventRequest;
import br.com.conectabem.dto.event.CreateAnnouncementRequest;
import br.com.conectabem.dto.event.EventAnnouncementDTO;
import br.com.conectabem.dto.event.EventReportDTO;
import br.com.conectabem.dto.event.UpdateEventRequest;
import br.com.conectabem.model.Event;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventService {

    Event create(CreateEventRequest request, UUID ownerId);

    List<Event> findAllByOwner(UUID ownerId);

    List<Event> findAvailable(String location, String activityType, String fromDate);

    Optional<Event> findOwnedById(UUID eventId, UUID ownerId);

    Optional<Event> findAccessibleById(UUID eventId, UUID userId);

    Optional<Event> update(UUID eventId, UpdateEventRequest request, UUID ownerId);

    boolean delete(UUID eventId, UUID requesterId);

    EventAnnouncementDTO createAnnouncement(UUID eventId,
                                            CreateAnnouncementRequest request,
                                            UUID ownerId);

    List<EventAnnouncementDTO> listAnnouncements(UUID eventId, UUID userId);

    EventReportDTO buildReport(UUID eventId, UUID ownerId);

    long getAvailableSpots(UUID eventId);
}
