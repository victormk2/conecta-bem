package br.com.conectabem.repository;

import br.com.conectabem.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {
    List<Event> findAllByOwnerIdOrderByStartsAtAsc(UUID ownerId);

    List<Event> findAllByOrderByStartsAtAsc();

    Optional<Event> findByIdAndOwnerId(UUID id, UUID ownerId);
}
