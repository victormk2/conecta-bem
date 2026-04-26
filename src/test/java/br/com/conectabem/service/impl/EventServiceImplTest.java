package br.com.conectabem.service.impl;

import br.com.conectabem.dto.event.EventCreationDTO;
import br.com.conectabem.dto.event.EventListRequest;
import br.com.conectabem.infra.util.Mapper;
import br.com.conectabem.model.Address;
import br.com.conectabem.model.Event;
import br.com.conectabem.model.EventCategory;
import br.com.conectabem.model.User;
import br.com.conectabem.repository.EventRepository;
import br.com.conectabem.service.AddressService;
import br.com.conectabem.service.CurrentUserService;
import br.com.conectabem.service.UserService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private Mapper<EventCreationDTO, Event> creationToEntity;

    @Mock
    private UserService userService;

    @Mock
    private AddressService addressService;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private EventServiceImpl eventService;

    @Nested
    class CreateTest {
        @Test
        void shouldCreateEventSuccessfully() {
            var dto = new EventCreationDTO(
                    "Limpeza do Parque",
                    "Vamos limpar o parque",
                    "00000000-0000-0000-0000-000000000002",
                    "ENVIRONMENT",
                    "2026-04-15T10:00:00",
                    "2026-04-15T14:00:00",
                    50
            );

            var ownerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            var addressId = UUID.fromString("00000000-0000-0000-0000-000000000002");

            var owner = new User();
            owner.setId(ownerId);
            owner.setUsername("john_doe");

            var address = new Address();
            address.setId(addressId);
            address.setCity("São Paulo");

            var eventFromMapper = new Event();
            eventFromMapper.setTitle(dto.title());
            eventFromMapper.setDescription(dto.description());
            eventFromMapper.setCapacity(dto.capacity());
            eventFromMapper.setCategory(EventCategory.ENVIRONMENT);
            eventFromMapper.setStartsAt(LocalDateTime.parse(dto.startsAt()));
            eventFromMapper.setEndsAt(LocalDateTime.parse(dto.endsAt()));

            var savedEvent = new Event();
            savedEvent.setId(UUID.randomUUID());
            savedEvent.setTitle(dto.title());
            savedEvent.setDescription(dto.description());
            savedEvent.setCapacity(dto.capacity());
            savedEvent.setCategory(EventCategory.ENVIRONMENT);
            savedEvent.setStartsAt(LocalDateTime.parse(dto.startsAt()));
            savedEvent.setEndsAt(LocalDateTime.parse(dto.endsAt()));
            savedEvent.setOwner(owner);
            savedEvent.setAddress(address);

            when(creationToEntity.map(dto)).thenReturn(eventFromMapper);
            when(currentUserService.requireUserId()).thenReturn(ownerId);
            when(userService.findById(ownerId)).thenReturn(owner);
            when(addressService.findById(addressId)).thenReturn(address);
            when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

            var result = eventService.create(dto);

            assertThat(result)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("title", "Limpeza do Parque")
                    .hasFieldOrPropertyWithValue("description", "Vamos limpar o parque")
                    .hasFieldOrPropertyWithValue("capacity", 50)
                    .hasFieldOrPropertyWithValue("category", EventCategory.ENVIRONMENT)
                    .hasFieldOrProperty("owner")
                    .hasFieldOrProperty("address");

            assertThat(result.getOwner()).hasFieldOrPropertyWithValue("id", ownerId);
            assertThat(result.getAddress()).hasFieldOrPropertyWithValue("id", addressId);

            verify(creationToEntity).map(dto);
            verify(userService).findById(ownerId);
            verify(addressService).findById(addressId);
            verify(eventRepository).save(any(Event.class));
        }

        @Test
        void shouldIgnoreImageWhenFileIsEmpty() {
            var dto = new EventCreationDTO(
                    "Evento com imagem",
                    "Descricao",
                    "00000000-0000-0000-0000-000000000002",
                    "SOCIAL",
                    "2026-04-15T10:00:00",
                    "2026-04-15T14:00:00",
                    20
            );

            var ownerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            var addressId = UUID.fromString("00000000-0000-0000-0000-000000000002");
            var owner = new User();
            owner.setId(ownerId);

            var address = new Address();
            address.setId(addressId);

            var eventFromMapper = new Event();
            var image = mock(MultipartFile.class);

            when(image.isEmpty()).thenReturn(true);
            when(creationToEntity.map(dto)).thenReturn(eventFromMapper);
            when(currentUserService.requireUserId()).thenReturn(ownerId);
            when(userService.findById(ownerId)).thenReturn(owner);
            when(addressService.findById(addressId)).thenReturn(address);
            when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

            var result = eventService.createWithImage(dto, image);

            assertThat(result).isSameAs(eventFromMapper);
            assertThat(result.getImage()).isNull();
            verify(eventRepository).save(any(Event.class));
        }

        @Test
        void shouldAttachImageWhenValidImageIsProvided() throws Exception {
            var dto = new EventCreationDTO(
                    "Evento com imagem",
                    "Descricao",
                    "00000000-0000-0000-0000-000000000002",
                    "SOCIAL",
                    "2026-04-15T10:00:00",
                    "2026-04-15T14:00:00",
                    20
            );

            var ownerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            var addressId = UUID.fromString("00000000-0000-0000-0000-000000000002");
            var imageBytes = new byte[]{1, 2, 3};

            var owner = new User();
            owner.setId(ownerId);

            var address = new Address();
            address.setId(addressId);

            var eventFromMapper = new Event();
            var image = mock(MultipartFile.class);

            when(image.isEmpty()).thenReturn(false);
            when(image.getContentType()).thenReturn("image/png");
            when(image.getBytes()).thenReturn(imageBytes);
            when(creationToEntity.map(dto)).thenReturn(eventFromMapper);
            when(currentUserService.requireUserId()).thenReturn(ownerId);
            when(userService.findById(ownerId)).thenReturn(owner);
            when(addressService.findById(addressId)).thenReturn(address);
            when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

            var result = eventService.createWithImage(dto, image);

            assertThat(result.getImage()).containsExactly(imageBytes);
            verify(eventRepository).save(any(Event.class));
        }
    }

    @Nested
    class ListTest {
        @Test
        void shouldReturnListOfEvents() {
            var event1 = new Event();
            event1.setId(UUID.randomUUID());
            event1.setTitle("Limpeza do Parque");
            event1.setCategory(EventCategory.ENVIRONMENT);
            event1.setImage(new byte[]{1, 2, 3});

            var event2 = new Event();
            event2.setId(UUID.randomUUID());
            event2.setTitle("Doação de Alimentos");
            event2.setCategory(EventCategory.SOCIAL);

            var request = new EventListRequest();

            when(eventRepository.findAll()).thenReturn(List.of(event1, event2));
            when(eventRepository.count()).thenReturn(2L);

            var result = eventService.list(request);

            assertThat(result)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("total", 2L);

            assertThat(result.events())
                    .hasSize(2)
                    .containsExactly(event1, event2);

            assertThat(result.events().get(0).getImage()).containsExactly(1, 2, 3);
        }

        @Test
        void shouldReturnEmptyListWhenNoEvents() {
            var request = new EventListRequest();

            when(eventRepository.findAll()).thenReturn(List.of());
            when(eventRepository.count()).thenReturn(0L);

            var result = eventService.list(request);

            assertThat(result)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("total", 0L);

            assertThat(result.events()).isEmpty();
        }

        @Test
        void shouldReturnCorrectPaginationValues() {
            var request = new EventListRequest();

            when(eventRepository.findAll()).thenReturn(List.of());
            when(eventRepository.count()).thenReturn(25L);

            var result = eventService.list(request);

            assertThat(result)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("total", 25L);
        }
    }

    @Nested
    class RemoveImageTest {
        @Test
        void shouldRemoveImageWhenEventExists() {
            var eventId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            var event = new Event();
            event.setId(eventId);
            event.setImage(new byte[]{1, 2, 3});

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

            var result = eventService.removeImageByEventId(eventId.toString());

            assertThat(result).isTrue();
            assertThat(event.getImage()).isNull();
            verify(eventRepository).save(event);
        }

        @Test
        void shouldReturnFalseWhenEventDoesNotExist() {
            var eventId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

            var result = eventService.removeImageByEventId(eventId.toString());

            assertThat(result).isFalse();
            verify(eventRepository, never()).save(any(Event.class));
        }
    }

    @Nested
    class FindByIdTest {
        @Test
        void shouldReturnEventWhenFound() {
            var eventId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            var event = new Event();
            event.setId(eventId);
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

            var result = eventService.findById(eventId.toString());

            assertThat(result).isEqualTo(event);
        }

        @Test
        void shouldReturnNullWhenEventDoesNotExist() {
            var eventId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

            var result = eventService.findById(eventId.toString());

            assertThat(result).isNull();
        }
    }
}

