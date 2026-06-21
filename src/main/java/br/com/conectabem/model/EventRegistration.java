package br.com.conectabem.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "event_registrations",
        uniqueConstraints = @UniqueConstraint(name = "ux_event_registrations_event_volunteer", columnNames = {"event", "volunteer"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "volunteer", nullable = false)
    private User volunteer;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ParticipationStatus status;

    @Column(name = "justification", length = 2000)
    private String justification;

    @Column(name = "organizer_feedback", length = 3000)
    private String organizerFeedback;

    @Column(name = "feedback_rating")
    private Integer feedbackRating;

    @Column(name = "feedback_created_at")
    private Instant feedbackCreatedAt;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private Instant registeredAt;

    @Column(name = "status_updated_at")
    private Instant statusUpdatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.registeredAt == null) {
            this.registeredAt = Instant.now();
        }
    }
}
