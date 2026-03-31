package br.com.conectabem.service.impl;

import br.com.conectabem.dto.address.AddressCreationDTO;
import br.com.conectabem.dto.address.AddressListRequest;
import br.com.conectabem.infra.util.Mapper;
import br.com.conectabem.model.Address;
import br.com.conectabem.repository.AddressRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private Mapper<AddressCreationDTO, Address> addressMapper;

    @InjectMocks
    private AddressServiceImpl addressService;

    @Nested
    class FindByIdTest {
        @Test
        void shouldReturnAddressWhenExists() {
            var addressId = UUID.randomUUID();
            var address = new Address();
            address.setId(addressId);
            address.setCountry("Brazil");
            address.setCity("São Paulo");
            address.setState("SP");
            address.setStreet("Rua das Flores");
            address.setNumber("123");
            address.setPostalCode("12345-678");

            when(addressRepository.findById(addressId)).thenReturn(Optional.of(address));

            var result = addressService.findById(addressId);

            assertThat(result)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("id", addressId)
                    .hasFieldOrPropertyWithValue("country", "Brazil")
                    .hasFieldOrPropertyWithValue("city", "São Paulo")
                    .hasFieldOrPropertyWithValue("state", "SP")
                    .hasFieldOrPropertyWithValue("street", "Rua das Flores")
                    .hasFieldOrPropertyWithValue("number", "123")
                    .hasFieldOrPropertyWithValue("postalCode", "12345-678");
        }

        @Test
        void shouldReturnNullWhenAddressDoesNotExist() {
            var addressId = UUID.randomUUID();

            when(addressRepository.findById(addressId)).thenReturn(Optional.empty());

            var result = addressService.findById(addressId);

            assertThat(result).isNull();
        }
    }

    @Nested
    class CreateTest {
        @Test
        void shouldCreateAddressSuccessfully() {
            var dto = new AddressCreationDTO(
                    "Brazil",
                    "Rio de Janeiro",
                    "RJ",
                    "Avenida Paulista",
                    "Apt 200",
                    "456",
                    "Bela Vista",
                    "Near metro",
                    "98765-432"
            );

            var addressToSave = new Address();
            addressToSave.setCountry(dto.country());
            addressToSave.setCity(dto.city());
            addressToSave.setState(dto.state());
            addressToSave.setStreet(dto.street());
            addressToSave.setComplement(dto.complement());
            addressToSave.setNumber(dto.number());
            addressToSave.setNeighborhood(dto.neighborhood());
            addressToSave.setReference(dto.reference());
            addressToSave.setPostalCode(dto.postalCode());

            var savedAddress = new Address();
            savedAddress.setId(UUID.randomUUID());
            savedAddress.setCountry(dto.country());
            savedAddress.setCity(dto.city());
            savedAddress.setState(dto.state());
            savedAddress.setStreet(dto.street());
            savedAddress.setComplement(dto.complement());
            savedAddress.setNumber(dto.number());
            savedAddress.setNeighborhood(dto.neighborhood());
            savedAddress.setReference(dto.reference());
            savedAddress.setPostalCode(dto.postalCode());

            when(addressMapper.map(dto)).thenReturn(addressToSave);
            when(addressRepository.save(any(Address.class))).thenReturn(savedAddress);

            var result = addressService.create(dto);

            assertThat(result)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("country", "Brazil")
                    .hasFieldOrPropertyWithValue("city", "Rio de Janeiro")
                    .hasFieldOrPropertyWithValue("state", "RJ")
                    .hasFieldOrPropertyWithValue("street", "Avenida Paulista")
                    .hasFieldOrPropertyWithValue("complement", "Apt 200")
                    .hasFieldOrPropertyWithValue("number", "456")
                    .hasFieldOrPropertyWithValue("neighborhood", "Bela Vista")
                    .hasFieldOrPropertyWithValue("reference", "Near metro")
                    .hasFieldOrPropertyWithValue("postalCode", "98765-432");

            verify(addressMapper).map(dto);
            verify(addressRepository).save(any(Address.class));
        }
    }

    @Nested
    class ListTest {
        @Test
        void shouldReturnListOfAddresses() {
            var address1 = new Address();
            address1.setId(UUID.randomUUID());
            address1.setCountry("Brazil");
            address1.setCity("São Paulo");

            var address2 = new Address();
            address2.setId(UUID.randomUUID());
            address2.setCountry("Brazil");
            address2.setCity("Rio de Janeiro");

            var request = new AddressListRequest();

            when(addressRepository.findAll()).thenReturn(List.of(address1, address2));
            when(addressRepository.count()).thenReturn(2L);

            var result = addressService.list(request);

            assertThat(result)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("total", 2L);

            assertThat(result.addresses())
                    .hasSize(2)
                    .containsExactly(address1, address2);
        }

        @Test
        void shouldReturnEmptyListWhenNoAddresses() {
            var request = new AddressListRequest();

            when(addressRepository.findAll()).thenReturn(List.of());
            when(addressRepository.count()).thenReturn(0L);

            var result = addressService.list(request);

            assertThat(result)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("total", 0L);

            assertThat(result.addresses()).isEmpty();
        }
    }
}


