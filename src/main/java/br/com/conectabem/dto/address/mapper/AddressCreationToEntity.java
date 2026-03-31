package br.com.conectabem.dto.address.mapper;

import br.com.conectabem.dto.address.AddressCreationDTO;
import br.com.conectabem.infra.util.Mapper;
import br.com.conectabem.model.Address;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class AddressCreationToEntity implements Mapper<AddressCreationDTO, Address> {
    @Override
    public Address map(AddressCreationDTO source) {
        if (Objects.nonNull(source)) {
            var address = new Address();
            address.setCountry(source.country());
            address.setCity(source.city());
            address.setState(source.state());
            address.setStreet(source.street());
            address.setComplement(source.complement());
            address.setNumber(source.number());
            address.setNeighborhood(source.neighborhood());
            address.setReference(source.reference());
            address.setPostalCode(source.postalCode());
            return address;
        }
        return null;
    }
}
