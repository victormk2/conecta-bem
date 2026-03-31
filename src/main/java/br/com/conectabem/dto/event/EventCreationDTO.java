package br.com.conectabem.dto.event;

public record EventCreationDTO(
        String title,
        String description,
        String addressId,
        String category,
        String startsAt,
        String endsAt,
        Integer capacity,
        String ownerId) {
}
