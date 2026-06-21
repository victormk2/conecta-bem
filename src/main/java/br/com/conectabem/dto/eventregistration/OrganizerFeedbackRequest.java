package br.com.conectabem.dto.eventregistration;

public record OrganizerFeedbackRequest(
        Integer rating,
        String comment
) {
}
