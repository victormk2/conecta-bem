package br.com.conectabem.service.impl;

import br.com.conectabem.dto.event.EventCreationDTO;
import br.com.conectabem.dto.event.EventListRequest;
import br.com.conectabem.dto.event.EventListResponse;
import br.com.conectabem.infra.util.Mapper;
import br.com.conectabem.model.Event;
import br.com.conectabem.repository.EventRepository;
import br.com.conectabem.service.AddressService;
import br.com.conectabem.service.CurrentUserService;
import br.com.conectabem.service.EventService;
import br.com.conectabem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final Mapper<EventCreationDTO, Event> creationToEntity;
    private final UserService userService;
    private final AddressService addressService;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public Event create(EventCreationDTO eventCreationDTO) {
        var baseEntity = creationToEntity.map(eventCreationDTO);
        baseEntity.setOwner(userService.findById(currentUserService.requireUserId()));
        baseEntity.setAddress(addressService.findById(UUID.fromString(eventCreationDTO.addressId())));
        return eventRepository.save(baseEntity);
    }

    @Override
    public EventListResponse list(EventListRequest eventListRequest) {
        // Alterar para um chamada com limit por page e offset e adicionar chamada separada para count total
        return new EventListResponse(eventRepository.findAll(), eventRepository.count());
    }
}
