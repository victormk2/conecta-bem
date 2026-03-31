package br.com.conectabem.service;

import br.com.conectabem.dto.address.AddressCreationDTO;
import br.com.conectabem.dto.address.AddressListRequest;
import br.com.conectabem.dto.address.AddressListResponse;
import br.com.conectabem.model.Address;

import java.util.UUID;

public interface AddressService {

    Address findById(UUID id);

    Address create(AddressCreationDTO addressCreationDTO);

    AddressListResponse list(AddressListRequest request);
}
