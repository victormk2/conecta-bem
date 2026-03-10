package br.com.conectabem.dto.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.Instant;

@Data
public class CreateEventRequest {

    @NotBlank
    private String title;

    private String description;

    @NotBlank
    private String location;

    @NotBlank
    private String activityType;

    @NotNull
    private Instant startsAt;

    private Instant endsAt;

    @Positive
    private Integer capacity;
}
