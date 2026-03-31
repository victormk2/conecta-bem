package br.com.conectabem.dto.address.mapper;

import br.com.conectabem.dto.address.AddressCreationDTO;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AddressCreationToEntityTest {

    @InjectMocks
    private AddressCreationToEntity addressCreationToEntity;

    @Nested
    class MapTest {
        @Test
        void shouldReturnCorrectObject() {
            var source = new AddressCreationDTO(
                    "Brazil",
                    "São Paulo",
                    "SP",
                    "Rua das Flores",
                    "Apt 101",
                    "123",
                    "Jardim das Acácias",
                    "Near the park",
                    "12345-678"
            );

            var result = addressCreationToEntity.map(source);

            assertThat(result)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("country", "Brazil")
                    .hasFieldOrPropertyWithValue("city", "São Paulo")
                    .hasFieldOrPropertyWithValue("state", "SP")
                    .hasFieldOrPropertyWithValue("street", "Rua das Flores")
                    .hasFieldOrPropertyWithValue("complement", "Apt 101")
                    .hasFieldOrPropertyWithValue("number", "123")
                    .hasFieldOrPropertyWithValue("neighborhood", "Jardim das Acácias")
                    .hasFieldOrPropertyWithValue("reference", "Near the park")
                    .hasFieldOrPropertyWithValue("postalCode", "12345-678");
        }

        @Test
        void shouldReturnNullWhenSourceIsNull() {
            var result = addressCreationToEntity.map(null);

            assertThat(result).isNull();
        }
    }
}