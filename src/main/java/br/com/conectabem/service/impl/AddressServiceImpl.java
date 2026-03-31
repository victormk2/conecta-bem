package br.com.conectabem.service.impl;

import br.com.conectabem.dto.address.AddressCreationDTO;
import br.com.conectabem.dto.address.AddressListRequest;
import br.com.conectabem.dto.address.AddressListResponse;
import br.com.conectabem.infra.util.Mapper;
import br.com.conectabem.model.Address;
import br.com.conectabem.repository.AddressRepository;
import br.com.conectabem.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final Mapper<AddressCreationDTO, Address> addressCreationDTOAddressMapper;

    @Override
    @Transactional(readOnly = true)
    public Address findById(UUID id) {
        return addressRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public Address create(AddressCreationDTO addressCreationDTO) {
        return addressRepository.save(addressCreationDTOAddressMapper.map(addressCreationDTO));
    }

    @Override
    @Transactional
    public AddressListResponse list(AddressListRequest request) {
        return new AddressListResponse(addressRepository.findAll(), addressRepository.count());
    }
}
