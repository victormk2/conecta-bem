package br.com.conectabem.repository;

import br.com.conectabem.model.EventAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventAnnouncementRepository extends JpaRepository<EventAnnouncement, UUID> {
    List<EventAnnouncement> findAllByEventIdOrderByCreatedAtDesc(UUID eventId);
}
