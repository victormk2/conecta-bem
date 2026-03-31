package br.com.conectabem.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "addresses")
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @Column(name = "country", nullable = false, length = 255)
    private String country;

    @Column(name = "city", nullable = false, length = 255)
    private String city;

    @Column(name = "state", nullable = false, length = 255)
    private String state;

    @Column(name = "street", nullable = false, length = 255)
    private String street;

    @Column(name = "complement", length = 255)
    private String complement;

    @Column(name = "number", nullable = false, length = 20)
    private String number;

    @Column(name = "neighborhood", length = 255)
    private String neighborhood;

    @Column(name = "reference", length = 255)
    private String reference;

    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

