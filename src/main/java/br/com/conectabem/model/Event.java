package br.com.conectabem.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(length = 3000)
    private String description;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private String activityType;

    @Column(nullable = false)
    private Instant startsAt;

    private Instant endsAt;

    private Integer capacity;

    @Column(nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant updatedAt;

}
