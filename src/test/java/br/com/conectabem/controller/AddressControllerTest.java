package br.com.conectabem.controller;

import br.com.conectabem.dto.address.AddressCreationDTO;
import br.com.conectabem.dto.address.AddressListRequest;
import br.com.conectabem.service.AddressService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AddressControllerTest {

    @InjectMocks
    private AddressController addressController;
    @Mock
    private AddressService addressService;

    @Nested
    class ListTest {
        @Test
        void shouldCallService() {
            var input = new AddressListRequest();
            var response = addressController.list(input);
            verify(addressService).list(input);
            Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
        }
    }

    @Nested
    class CreateTest {
        @Test
        void shouldCallService() {
            var request = new AddressCreationDTO("country", "city", "state",
                    "street", "complement", "number",
                    "neighborhood", "reference", "postalCode");
            var response = addressController.createAddress(request);
            verify(addressService).create(request);
            Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(201));
        }
    }

}