package br.com.conectabem.repository;

import br.com.conectabem.model.EventRegistration;
import br.com.conectabem.model.ParticipationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRegistrationRepository extends JpaRepository<EventRegistration, UUID> {
    Optional<EventRegistration> findByEventIdAndVolunteerId(UUID eventId, UUID volunteerId);

    List<EventRegistration> findAllByEventIdOrderByRegisteredAtAsc(UUID eventId);

    List<EventRegistration> findAllByVolunteerIdOrderByRegisteredAtDesc(UUID volunteerId);

    long countByEventIdAndStatusIn(UUID eventId, Collection<ParticipationStatus> statuses);
}
