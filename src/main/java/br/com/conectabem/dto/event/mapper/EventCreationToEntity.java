package br.com.conectabem.dto.event.mapper;

import br.com.conectabem.dto.event.EventCreationDTO;
import br.com.conectabem.infra.util.Mapper;
import br.com.conectabem.model.Event;
import br.com.conectabem.model.EventCategory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
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
            event.setStartsAt(LocalDateTime.parse(source.startsAt()));
            event.setEndsAt(LocalDateTime.parse(source.endsAt()));
            event.setCategory(EventCategory.valueOf(source.category()));
            return event;
        }

        return null;
    }
}
