package br.com.conectabem.dto.event.mapper;

import br.com.conectabem.dto.event.EventCreationDTO;
import br.com.conectabem.infra.util.Mapper;
import br.com.conectabem.model.Event;
import br.com.conectabem.model.EventCategory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Objects;

@Component
public class EventCreationToEntity implements Mapper<EventCreationDTO, Event> {

    @Override
    public Event map(EventCreationDTO source) {
        if (Objects.nonNull(source)) {
            var event = new Event();
            event.setTitle(source.title());
            event.setDescription(source.description());
            event.setCapacity(source.capacity());

            try {
                if (source.startsAt() != null) {
                    event.setStartsAt(LocalDateTime.parse(source.startsAt()));
                }
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid 'startsAt' date format. Expected format: yyyy-MM-ddTHH:mm:ss (e.g., 2026-04-15T10:00:00)");
            }

            try {
                if (source.endsAt() != null) {
                    event.setEndsAt(LocalDateTime.parse(source.endsAt()));
                }
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid 'endsAt' date format. Expected format: yyyy-MM-ddTHH:mm:ss (e.g., 2026-04-15T14:00:00)");
            }

            try {
                event.setCategory(EventCategory.valueOf(source.category()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid category. Valid values are: " + String.join(", ",
                    java.util.Arrays.stream(EventCategory.values()).map(Enum::name).toArray(String[]::new)));
            }

            return event;
        }

        return null;
    }
}
