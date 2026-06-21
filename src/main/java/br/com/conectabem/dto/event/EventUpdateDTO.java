package br.com.conectabem.dto.event;

public record EventUpdateDTO(
        String id,
        String title,
        String description,
        String addressId,
        String category,
        String startsAt,
        String endsAt,
        Integer capacity,
        String type,
        String organizationName,
        String organizationDocument) {

    public EventUpdateDTO(
            String id,
            String title,
            String description,
            String addressId,
            String category,
            String startsAt,
            String endsAt,
            Integer capacity
    ) {
        this(id, title, description, addressId, category, startsAt, endsAt, capacity, null, null, null);
    }
}
