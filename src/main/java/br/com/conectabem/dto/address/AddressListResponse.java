package br.com.conectabem.dto.address;

import br.com.conectabem.model.Address;

import java.util.List;

public record AddressListResponse(
        List<Address> addresses,
        long total
) {
}
