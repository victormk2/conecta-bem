package br.com.conectabem.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "event_registrations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRegistration {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID eventId;

    @Column(nullable = false)
    private UUID volunteerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipationStatus status;

    @Column(length = 2000)
    private String justification;

    @Column(nullable = false)
    private Instant registeredAt;

    private Instant statusUpdatedAt;
}
