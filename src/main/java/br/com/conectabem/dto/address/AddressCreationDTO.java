package br.com.conectabem.dto.address;

public record AddressCreationDTO(
        String country,
        String city,
        String state,
        String street,
        String complement,
        String number,
        String neighborhood,
        String reference,
        String postalCode
) {
}
