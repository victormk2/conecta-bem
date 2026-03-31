package br.com.conectabem.dto.event.mapper;

import br.com.conectabem.dto.event.EventCreationDTO;
import br.com.conectabem.model.EventCategory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EventCreationToEntityTest {

    @InjectMocks
    private EventCreationToEntity eventCreationToEntity;

    @Nested
    class MapTest {
        @Test
        void shouldReturnCorrectObject() {
            var startsAt = "2026-04-15T10:00:00";
            var endsAt = "2026-04-15T14:00:00";
            var source = new EventCreationDTO(
                    "Limpeza do Parque",
                    "Vamos limpar o parque e plantar árvores",
                    "uuid-address-123",
                    "ENVIRONMENT",
                    startsAt,
                    endsAt,
                    50,
                    "uuid-owner-456"
            );

            var result = eventCreationToEntity.map(source);

            assertThat(result)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("title", "Limpeza do Parque")
                    .hasFieldOrPropertyWithValue("description", "Vamos limpar o parque e plantar árvores")
                    .hasFieldOrPropertyWithValue("capacity", 50)
                    .hasFieldOrPropertyWithValue("category", EventCategory.ENVIRONMENT)
                    .hasFieldOrPropertyWithValue("startsAt", LocalDateTime.parse(startsAt))
                    .hasFieldOrPropertyWithValue("endsAt", LocalDateTime.parse(endsAt));
        }

        @Test
        void shouldMapDifferentCategories() {
            var source = new EventCreationDTO(
                    "Doação de Alimentos",
                    "Arrecadação de alimentos",
                    "uuid-address-123",
                    "SOCIAL",
                    "2026-05-01T09:00:00",
                    "2026-05-01T12:00:00",
                    100,
                    "uuid-owner-456"
            );

            var result = eventCreationToEntity.map(source);

            assertThat(result)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("category", EventCategory.SOCIAL);
        }

        @Test
        void shouldReturnNullWhenSourceIsNull() {
            var result = eventCreationToEntity.map(null);

            assertThat(result).isNull();
        }
    }
}