package br.com.conectabem.dto.event;

public record EventUpdateDTO(
        String id,
        String title,
        String description,
        String addressId,
        String category,
        String startsAt,
        String endsAt,
        Integer capacity) {
}
