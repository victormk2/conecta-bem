package br.com.conectabem.dto.event;

public record EventCreationDTO(
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

    public EventCreationDTO(
            String title,
            String description,
            String addressId,
            String category,
            String startsAt,
            String endsAt,
            Integer capacity
    ) {
        this(title, description, addressId, category, startsAt, endsAt, capacity, null, null, null);
    }
}
